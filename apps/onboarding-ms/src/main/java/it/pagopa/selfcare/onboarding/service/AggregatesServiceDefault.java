package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
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

    private final ExpiringMap<String, GeographicTaxonomyFromIstatCode> expiringMap;

    @Inject
    public AggregatesServiceDefault(@ConfigProperty(name = "onboarding-ms.istat-cache-duration-minutes") int cacheDuration) {
        this.expiringMap = ExpiringMap.builder()
                .expiration(cacheDuration, TimeUnit.MINUTES)
                .build();
    }

    public static final String LOG_CSV_ROWS = "CSV file validated end: %s valid row and %s invalid row";
    protected static final String DESCRIPTION_TO_REPLACE_REGEX = " - COMUNE";
    public static final String ERROR_IPA = "Codice fiscale non presente su IPA";
    public static final String ERROR_TAXCODE = "Il codice fiscale è obbligatorio";
    public static final String ERROR_SUBUNIT_TYPE = "SubunitType non valido";
    public static final String ERROR_AOO_UO = "In caso di AOO/UO è necessario specificare la tipologia e il codice univoco IPA AOO/UO";
    public static final String ERROR_VATNUMBER = "La partita IVA è obbligatoria";
    public static final String ERROR_ADMIN_NAME = "Nome Amministratore Ente Aggregato è obbligatorio";
    public static final String ERROR_ADMIN_SURNAME = "Cognome Amministratore Ente Aggregato è obbligatorio";
    public static final String ERROR_ADMIN_EMAIL = "Email Amministratore Ente Aggregato è obbligatorio";
    public static final String ERROR_ADMIN_TAXCODE = "Codice Fiscale Amministratore Ente Aggregato è obbligatorio";
    public static final String ERROR_TAXCODE_PT = "Codice Fiscale Partner Tecnologico è obbligatorio";
    public static final String ERROR_IBAN = "IBAN è obbligatorio";
    public static final String ERROR_SERVICE = "Servizio è obbligatorio";
    public static final String ERROR_SYNC_ASYNC_MODE = "Modalità Sincrona/Asincrona è obbligatorio";
    public static final String ERROR_CODICE_SDI = "Codice SDI è obbligatorio";
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
    public Uni<VerifyAggregateResponse> validatePagoPaAggregatesCsv(File file) {
        AggregatesCsv<CsvAggregatePagoPa> aggregatesCsv = csvService.readItemsFromCsv(file, CsvAggregatePagoPa.class);
        List<CsvAggregatePagoPa> csvAggregates = aggregatesCsv.getCsvAggregateList();
        VerifyAggregateResponse verifyAggregatePagoPaResponse = new VerifyAggregateResponse();

        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregatePagoPa -> checkCsvAggregatePagoPaAndFillAggregateOrErrorList(csvAggregatePagoPa, verifyAggregatePagoPaResponse))
                .collect().asList()
                .replaceWith(verifyAggregatePagoPaResponse)
                .onItem().invoke(() -> LOG.infof(LOG_CSV_ROWS,
                        verifyAggregatePagoPaResponse.getAggregates().size(),
                        verifyAggregatePagoPaResponse.getErrors().size()));
    }

    @Override
    public Uni<VerifyAggregateResponse> validateSendAggregatesCsv(File file) {
        AggregatesCsv<CsvAggregateSend> aggregatesCsv = csvService.readItemsFromCsv(file, CsvAggregateSend.class);
        List<CsvAggregateSend> csvAggregates = aggregatesCsv.getCsvAggregateList();
        VerifyAggregateResponse verifyAggregateSendResponse = new VerifyAggregateResponse();

        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregateSend -> checkCsvAggregateSendAndFillAggregateOrErrorList(csvAggregateSend, verifyAggregateSendResponse))
                .collect().asList()
                .replaceWith(verifyAggregateSendResponse)
                .onItem().invoke(() -> LOG.infof(LOG_CSV_ROWS,
                        verifyAggregateSendResponse.getAggregates().size(),
                        verifyAggregateSendResponse.getErrors().size()));
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

    private Uni<Void> checkCsvAggregatePagoPaAndFillAggregateOrErrorList(CsvAggregatePagoPa csvAggregatePagoPa, VerifyAggregateResponse verifyAggregatePagoPaResponse) {
        return checkCsvAggregatePagoPa(csvAggregatePagoPa)
                .onItem().invoke(aggregateSend -> verifyAggregatePagoPaResponse.getAggregates().add(aggregateSend))
                .onFailure(ResourceNotFoundException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregatePagoPaResponse.getErrors().add(mapToErrorRow(csvAggregatePagoPa.getRowNumber(), csvAggregatePagoPa.getTaxCode(), throwable));
                    return Uni.createFrom().nullItem();
                })
                .onFailure(InvalidRequestException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregatePagoPaResponse.getErrors().add(mapToErrorRow(csvAggregatePagoPa.getRowNumber(), csvAggregatePagoPa.getTaxCode(), throwable));
                    return Uni.createFrom().nullItem();
                })
                .replaceWithVoid();
    }

    private Uni<Void> checkCsvAggregateSendAndFillAggregateOrErrorList(CsvAggregateSend csvAggregateSend, VerifyAggregateResponse verifyAggregateSendResponse) {
        return checkCsvAggregateSend(csvAggregateSend)
                .onItem().invoke(aggregateSend -> verifyAggregateSendResponse.getAggregates().add(aggregateSend))
                .onFailure(ResourceNotFoundException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregateSendResponse.getErrors().add(mapToErrorRow(csvAggregateSend.getRowNumber(), csvAggregateSend.getTaxCode(), throwable));
                    return Uni.createFrom().nullItem();
                })
                .onFailure(InvalidRequestException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregateSendResponse.getErrors().add(mapToErrorRow(csvAggregateSend.getRowNumber(), csvAggregateSend.getTaxCode(), throwable));
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

    private Uni<Aggregate> checkCsvAggregateSend(CsvAggregateSend csvAggregateSend) {
        return checkRequiredFieldsSend(csvAggregateSend)
                .onItem().transformToUni(unused -> retrieveDataFromIpa(onboardingMapper.csvToAggregateSend(csvAggregateSend)));
    }

    private Uni<Aggregate> checkCsvAggregatePagoPa(CsvAggregatePagoPa csvAggregatePagoPa) {
        return checkRequiredFieldsPagoPa(csvAggregatePagoPa)
                .onItem().transformToUni(unused -> retrieveDataFromIpa(onboardingMapper.csvToAggregatePagoPa(csvAggregatePagoPa)));
    }

    private Uni<Aggregate> retrieveDataFromIpa(Aggregate aggregate) {
        aggregate.setOrigin(InstitutionResource.OriginEnum.IPA.value());

        if (StringUtils.isEmpty(aggregate.getSubunitType())) {
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

    private Uni<Void> checkRequiredFieldsPagoPa(CsvAggregatePagoPa csvAggregate) {

        if (StringUtils.isEmpty(csvAggregate.getTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE));
        } else if (StringUtils.isEmpty(csvAggregate.getVatNumber())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_VATNUMBER));
        } else if (StringUtils.isEmpty(csvAggregate.getTaxCodePT())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE_PT));
        } else if (StringUtils.isEmpty(csvAggregate.getIban())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_IBAN));
        } else if (StringUtils.isEmpty(csvAggregate.getService())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_SERVICE));
        } else if (StringUtils.isEmpty(csvAggregate.getSyncAsyncMode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_SYNC_ASYNC_MODE));
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> checkRequiredFieldsSend(CsvAggregateSend csvAggregate) {

        if (StringUtils.isEmpty(csvAggregate.getTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE));
        } else if (StringUtils.isEmpty(csvAggregate.getVatNumber())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_VATNUMBER));
        } else if ((StringUtils.isEmpty(csvAggregate.getSubunitType()) && StringUtils.isNotEmpty(csvAggregate.getSubunitCode()))
                || (StringUtils.isNotEmpty(csvAggregate.getSubunitType()) && StringUtils.isEmpty(csvAggregate.getSubunitCode()))) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_AOO_UO));
        } else if (StringUtils.isEmpty(csvAggregate.getAdminAggregateName())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_NAME));
        } else if (StringUtils.isEmpty(csvAggregate.getAdminAggregateSurname())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_SURNAME));
        } else if (StringUtils.isEmpty(csvAggregate.getAdminAggregateTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_TAXCODE));
        } else if (StringUtils.isEmpty(csvAggregate.getAdminAggregateEmail())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_EMAIL));
        } else if(StringUtils.isEmpty(csvAggregate.getRecipientCode())){
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_CODICE_SDI));

        }
        return Uni.createFrom().voidItem();
    }
}
