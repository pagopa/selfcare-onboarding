package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.conf.OnboardingMsConfig;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType.UO;

@ApplicationScoped
@Slf4j
public class AggregatesServiceDefault implements AggregatesService {

    private static final Logger LOG = Logger.getLogger(AggregatesServiceDefault.class);

    @RestClient
    @Inject
    AooApi aooApi;

    @RestClient
    @Inject
    InstitutionApi institutionApi;

    @RestClient
    @Inject
    UoApi uoApi;

    @RestClient
    @Inject
    GeographicTaxonomiesApi geographicTaxonomiesApi;

    @Inject
    OnboardingMapper onboardingMapper;

    @Inject
    CsvService csvService;

    private final AzureBlobClient azureBlobClient;
    private final OnboardingMsConfig onboardingMsConfig;
    private final ExpiringMap<String, GeographicTaxonomyFromIstatCode> expiringMap;

    public AggregatesServiceDefault (AzureBlobClient azureBlobClient, OnboardingMsConfig onboardingMsConfig){
        this.azureBlobClient = azureBlobClient;
        this.onboardingMsConfig = onboardingMsConfig;
        this.expiringMap = ExpiringMap.builder()
                .expiration(30, TimeUnit.MINUTES)
                .build();
    }

    public static final String LOG_CSV_ROWS = "CSV file validated end: %s valid row and %s invalid row";
    protected static final String DESCRIPTION_TO_REPLACE_REGEX = " - COMUNE";
    public static final String ERROR_IPA = "Codice fiscale non presente su IPA";
    public static final String ERROR_TAXCODE = "Il codice fiscale è obbligatorio";
    public static final String ERROR_SUBUNIT_TYPE = "SubunitType non valido";
    public static final String ERROR_AOO_UO = "In caso di AOO/UO è necessario specificare la tipologia e il codice univoco IPA AOO/UO";
    public static final String ERROR_VATNUMBER = "La partita IVA è obbligatoria";
    private static final String PEC = "Pec";


    @Override
    public Uni<VerifyAggregateResponse> validateAppIoAggregatesCsv(File file) {
        AggregatesCsv<CsvAggregateAppIo> aggregatesCsv = csvService.readItemsFromCsv(file, CsvAggregateAppIo.class);
        List<CsvAggregateAppIo> csvAggregates = aggregatesCsv.getCsvAggregateList();
        VerifyAggregateResponse verifyAggregateAppIoResponse = new VerifyAggregateResponse();

        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregateAppIo ->
                        checkCsvAggregateAppIoAndFillAggregateOrErrorList(csvAggregateAppIo, verifyAggregateAppIoResponse))
                .collect().asList()
                .replaceWith(verifyAggregateAppIoResponse)
                .onItem().invoke(() -> LOG.infof(LOG_CSV_ROWS,
                        verifyAggregateAppIoResponse.getAggregates().size(),
                        verifyAggregateAppIoResponse.getErrors().size()));
    }

    @Override
    public Uni<RestResponse<File>> retrieveAggregatesCsv(String onboardingId, String productId) {
        return Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(String.format("%s%s/%s/%s", onboardingMsConfig.getAggregatesPath(), onboardingId, productId, "aggregates.csv")))
                .runSubscriptionOn(Executors.newSingleThreadExecutor())
                .onItem().transform(csv -> {
                    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok(csv, MediaType.APPLICATION_OCTET_STREAM);
                    response.header("Content-Disposition", "attachment;filename=aggregates.csv");
                    return response.build();
                });
    }


    private Uni<Void> checkCsvAggregateAppIoAndFillAggregateOrErrorList(CsvAggregateAppIo csvAggregateAppIo, VerifyAggregateResponse verifyAggregateAppIoResponse) {

        return checkCsvAggregateAppIo(csvAggregateAppIo)
                .onItem().invoke(aggregateAppIo -> verifyAggregateAppIoResponse.getAggregates().add(aggregateAppIo))
                .onFailure(ResourceNotFoundException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregateAppIoResponse.getErrors().add(mapToErrorRow(csvAggregateAppIo.getRowNumber(), csvAggregateAppIo.getTaxCode(), throwable));
                    return Uni.createFrom().nullItem();
                })
                .onFailure(InvalidRequestException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregateAppIoResponse.getErrors().add(mapToErrorRow(csvAggregateAppIo.getRowNumber(), csvAggregateAppIo.getTaxCode(), throwable));
                    return Uni.createFrom().nullItem();
                })
                .replaceWithVoid();
    }

    private static RowError mapToErrorRow(Integer rowNumber, String taxCode, Throwable throwable) {
        return new RowError(rowNumber, taxCode, throwable.getMessage());
    }

    private Uni<Aggregate> checkCsvAggregateAppIo(CsvAggregateAppIo csvAggregateAppIo) {
        return checkRequiredFieldsAppIo(csvAggregateAppIo)
                .onItem().transformToUni(unused -> retrieveDataFromIpa(onboardingMapper.csvToAggregateAppIo(csvAggregateAppIo)));
    }

    private Uni<Aggregate> retrieveDataFromIpa(Aggregate aggregate) {
        aggregate.setOrigin(InstitutionResource.OriginEnum.IPA.value());

        if (Objects.isNull(aggregate.getSubunitType())){
            return institutionApi.findInstitutionUsingGET(aggregate.getTaxCode(), null, null)
                    .onFailure(this::checkIfNotFound).recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA)))
                    .onItem().transformToUni(institutionResource -> retrieveCityCountyAndMapIpaFieldForPA(institutionResource, aggregate));
        } else if (InstitutionPaSubunitType.AOO.name().equalsIgnoreCase(aggregate.getSubunitType())) {
            return aooApi.findByUnicodeUsingGET(aggregate.getSubunitCode(), null)
                    .onFailure(this::checkIfNotFound)
                    .recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA)))
                    .onItem().transformToUni(aooResource -> retrieveCityCountyAndMapIpaFieldForAOO(aooResource, aggregate));
        } else if (UO.name().equalsIgnoreCase(aggregate.getSubunitType())) {
            return uoApi.findByUnicodeUsingGET1(aggregate.getSubunitCode(), null)
                    .onFailure(this::checkIfNotFound)
                    .recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA)))
                    .onItem().transformToUni(uoResource -> retrieveCityCountyAndMapIpaFieldForUO(uoResource, aggregate));
        } else {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_SUBUNIT_TYPE));
        }
    }

    private Uni<Aggregate> retrieveCityCountyAndMapIpaFieldForUO(UOResource uoResource, Aggregate aggregateAppIo) {
        return retrieveGeographicTaxonomies(uoResource.getCodiceComuneISTAT())
                .onItem().transformToUni(geographicTaxonomyResource -> {
                    mapIpaField(uoResource.getDenominazioneEnte(), uoResource.getIndirizzo(), uoResource.getCap(), null, aggregateAppIo, geographicTaxonomyResource);
                    return retrieveDigitalAddress(uoResource.getTipoMail1(), uoResource.getMail1(), uoResource.getCodiceFiscaleEnte(), aggregateAppIo);
                });
    }

    private Uni<Aggregate> retrieveCityCountyAndMapIpaFieldForAOO(AOOResource aooResource, Aggregate aggregateAppIo) {
        return retrieveGeographicTaxonomies(aooResource.getCodiceComuneISTAT())
                .onItem().transformToUni(geographicTaxonomyResource -> {
                    mapIpaField(aooResource.getDenominazioneEnte(), aooResource.getIndirizzo(), aooResource.getCap(), null, aggregateAppIo, geographicTaxonomyResource);
                    return retrieveDigitalAddress(aooResource.getTipoMail1(), aooResource.getMail1(), aooResource.getCodiceFiscaleEnte(), aggregateAppIo);
                });
    }

    private Uni<Aggregate> retrieveCityCountyAndMapIpaFieldForPA(InstitutionResource institutionResource, Aggregate aggregateAppIo) {
        return retrieveGeographicTaxonomies(institutionResource.getIstatCode())
                .onItem().transform(geographicTaxonomyResource -> {
                    mapIpaField(institutionResource.getDescription(), institutionResource.getAddress(), institutionResource.getZipCode(), institutionResource.getOriginId(), aggregateAppIo, geographicTaxonomyResource);
                    aggregateAppIo.setDigitalAddress(institutionResource.getDigitalAddress());
                    return aggregateAppIo;
                });
    }

    private Uni<Aggregate> retrieveDigitalAddress(String mailType, String mail, String taxCode, Aggregate aggregateAppIo) {
        if (Objects.equals(mailType, PEC)) {
            aggregateAppIo.setDigitalAddress(mail);
            return Uni.createFrom().item(aggregateAppIo);
        } else {
            return institutionApi.findInstitutionUsingGET(taxCode, null, null)
                    .onItem().invoke(institutionResource -> aggregateAppIo.setDigitalAddress(institutionResource.getDigitalAddress()))
                    .replaceWith(aggregateAppIo);
        }
    }

    private static void mapIpaField(String description, String address, String zipCode, String originId, Aggregate aggregateAppIo, GeographicTaxonomyFromIstatCode geographicTaxonomyFromIstatCode) {
        if(Objects.nonNull(geographicTaxonomyFromIstatCode)) {
            aggregateAppIo.setCounty(geographicTaxonomyFromIstatCode.getCounty());
            aggregateAppIo.setCity(geographicTaxonomyFromIstatCode.getCity());
        }
        aggregateAppIo.setDescription(description);
        aggregateAppIo.setAddress(address);
        aggregateAppIo.setZipCode(zipCode);
        aggregateAppIo.setOriginId(originId);
    }

    private Uni<GeographicTaxonomyFromIstatCode> retrieveGeographicTaxonomies(String codiceIstat) {
        GeographicTaxonomyFromIstatCode geographicTaxonomyFromIstatCode = expiringMap.get(codiceIstat);

        if (Objects.isNull(geographicTaxonomyFromIstatCode)) {
            return geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(codiceIstat)
                    .map(geographicTaxonomyResource -> GeographicTaxonomyFromIstatCode.builder()
                            .city(Optional.ofNullable(geographicTaxonomyResource.getDesc()).orElse("").replace(DESCRIPTION_TO_REPLACE_REGEX, ""))
                            .county(geographicTaxonomyResource.getProvinceAbbreviation())
                            .build())
                    .onItem().invoke(entity -> expiringMap.put(codiceIstat, entity));
        }
        return Uni.createFrom().item(geographicTaxonomyFromIstatCode);
    }

    private boolean checkIfNotFound(Throwable throwable) {
        return throwable instanceof WebApplicationException webApplicationException && webApplicationException.getResponse().getStatus() == 404;
    }

    private Uni<Void> checkRequiredFieldsAppIo(CsvAggregateAppIo csvAggregateAppIo) {

        if (StringUtils.isEmpty(csvAggregateAppIo.getTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE));
        } else if (StringUtils.isEmpty(csvAggregateAppIo.getVatNumber())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_VATNUMBER));
        } else if ((StringUtils.isEmpty(csvAggregateAppIo.getSubunitType()) && StringUtils.isNotEmpty(csvAggregateAppIo.getSubunitCode()))
                || (StringUtils.isNotEmpty(csvAggregateAppIo.getSubunitType()) && StringUtils.isEmpty(csvAggregateAppIo.getSubunitCode()))) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_AOO_UO));
        }
        return Uni.createFrom().voidItem();
    }
}
