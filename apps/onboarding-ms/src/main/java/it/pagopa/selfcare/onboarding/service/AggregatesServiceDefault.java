package it.pagopa.selfcare.onboarding.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
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
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.GeographicTaxonomyResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.opencsv.ICSVParser.DEFAULT_QUOTE_CHARACTER;
import static it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType.UO;

@ApplicationScoped
@Slf4j
public class AggregatesServiceDefault implements AggregatesService {

    private static final Logger LOG = Logger.getLogger(AggregatesServiceDefault.class);

    protected static final String DESCRIPTION_TO_REPLACE_REGEX = " - COMUNE";

    protected ExpiringMap<String, GeographicTaxonomyFromIstatCode> expiringMap = ExpiringMap.builder()
            .expiration(7, TimeUnit.DAYS)
            .build();

    @Inject
    OnboardingMapper onboardingMapper;

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

    public static final String ERROR_READING_CSV = "Error reading CSV: ";
    public static final String MALFORMED_ROW = "Riga malformata";
    public static final String ERROR_IPA = "Codice fiscale non presente su IPA";
    public static final String ERROR_TAXCODE = "Il codice fiscale è obbligatorio";
    public static final String ERROR_DESCRIPTION = "La ragione sociale è obbligatoria";
    public static final String ERROR_SUBUNIT_TYPE = "SubunitType non valido";

    public static final String ERROR_AOO_UO = "In caso di AOO/UO è necessario specificare la tipologia e il codice univoco IPA AOO/UO";
    public static final String ERROR_VATNUMBER = "La partita IVA è obbligatoria";

    public static final String ERROR_ADDRESS = "Indirizzo è obbligatorio";
    public static final String ERROR_CITY = "La città è obbligatoria";
    public static final String ERROR_PROVINCE = "La provincia è obbligatoria";
    public static final String ERROR_PEC = "Indirizzo PEC è obbligatorio";
    public static final String ERROR_ADMIN_NAME = "Nome Amministratore Ente Aggregato è obbligatorio";
    public static final String ERROR_ADMIN_SURNAME = "Cognome Amministratore Ente Aggregato è obbligatorio";
    public static final String ERROR_ADMIN_EMAIL = "Email Amministratore Ente Aggregato è obbligatorio";
    public static final String ERROR_ADMIN_TAXCODE = "Codice Fiscale Amministratore Ente Aggregato è obbligatorio";
    public static final String ERROR_AGGREGATE_NAME_PT = "Ragine Sociale Partner Tecnologico è obbligatorio";
    public static final String ERROR_TAXCODE_PT = "Codice Fiscale Partner Tecnologico è obbligatorio";
    public static final String ERROR_IBAN = "IBAN è obbligatorio";
    public static final String ERROR_SERVICE = "Servizio è obbligatorio";
    public static final String ERROR_SYNC_ASYNC_MODE = "Modalità Sincrona/Asincrona è obbligatorio";
    private static final String ERROR_IPA_CODE = "Codice IPA è obbligatorio in caso di ente centrale";
    private static final String PEC = "Pec";


    @Override
    public Uni<VerifiyAggregateResponseInterface<AggregateAppIo>> validateAppIoAggregatesCsv(File file) {
        AggregatesCsv<CsvAggregateAppIo> aggregatesCsv = readItemsFromCsv(file, CsvAggregateAppIo.class);
        List<CsvAggregateAppIo> csvAggregates = aggregatesCsv.getCsvAggregateList();
        VerifiyAggregateResponseInterface<AggregateAppIo> verifyAggregateAppIoResponse = new VerifyAggregateAppIoResponse();

        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregateAppIo ->
                        checkCsvAggregateAppIoAndFillAggregateOrErrorList(csvAggregateAppIo, verifyAggregateAppIoResponse))
                .collect().asList()
                .replaceWith(verifyAggregateAppIoResponse)
                .onItem().invoke(() -> LOG.infof("CSV file validated end: %s valid row and %s invalid row",
                        verifyAggregateAppIoResponse.getAggregates().size(),
                        verifyAggregateAppIoResponse.getErrors().size()));
    }

    @Override
    public Uni<VerifyAggregateSendResponse> validateSendAggregatesCsv(File file) {
        AggregatesCsv<CsvAggregateSend> aggregatesCsv = readItemsFromCsv(file, CsvAggregateSend.class);
        List<CsvAggregateSend> csvAggregates = aggregatesCsv.getCsvAggregateList();
        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregate -> checkCsvAggregateSendAndFillAggregateOrErrorList(csvAggregate, aggregatesCsv))
                .collect().asList()
                .onItem().transform(list -> onboardingMapper.toVerifyAggregateSendResponse(aggregatesCsv))
                .onItem().invoke(() -> LOG.infof("CSV file validated end: %s valid row and %s invalid row",
                        aggregatesCsv.getValidAggregates().size(),
                        aggregatesCsv.getRowErrorList().size()));
    }

    @Override
    public Uni<VerifyAggregateResponse> validatePagoPaAggregatesCsv(File file) {
        AggregatesCsv<CsvAggregatePagoPa> aggregatesCsv = readItemsFromCsv(file, CsvAggregatePagoPa.class);
        List<CsvAggregatePagoPa> csvAggregates = aggregatesCsv.getCsvAggregateList();
        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregate -> checkCsvAggregatePagoPaAndFillAggregateOrErrorList(csvAggregate, aggregatesCsv))
                .collect().asList()
                .onItem().transform(list -> onboardingMapper.toVerifyAggregateResponse(aggregatesCsv))
                .onItem().invoke(() -> LOG.infof("CSV file validated end: %s valid row and %s invalid row",
                        aggregatesCsv.getValidAggregates().size(),
                        aggregatesCsv.getRowErrorList().size()));

    }

    private Uni<Void> checkCsvAggregateAppIoAndFillAggregateOrErrorList(CsvAggregateAppIo csvAggregateAppIo, VerifiyAggregateResponseInterface<AggregateAppIo> verifyAggregateAppIoResponse) {

        return checkCsvAggregateAppIo(csvAggregateAppIo)
                .onItem().invoke(aggregateAppIo -> verifyAggregateAppIoResponse.getAggregates().add(aggregateAppIo))
                .onFailure(ResourceNotFoundException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregateAppIoResponse.getErrors().add(mapToAppIoErrorRow(csvAggregateAppIo, throwable));
                    return Uni.createFrom().nullItem();
                })
                .onFailure(InvalidRequestException.class)
                .recoverWithUni(throwable -> {
                    verifyAggregateAppIoResponse.getErrors().add(mapToAppIoErrorRow(csvAggregateAppIo, throwable));
                    return Uni.createFrom().nullItem();
                })
                .replaceWithVoid();
    }

    private Uni<Void> checkCsvAggregateSendAndFillAggregateOrErrorList(Csv csv, AggregatesCsv aggregatesCsvResponse) {
        CsvAggregateSend csvAggregate = (CsvAggregateSend) csv;
        return checkCsvAggregateSend(csvAggregate)
                .onItem().invoke(() -> aggregatesCsvResponse.getValidAggregates().add(csvAggregate))
                .onFailure(ResourceNotFoundException.class).recoverWithUni(throwable -> mapToErrorRow(csvAggregate, throwable, aggregatesCsvResponse))
                .onFailure(InvalidRequestException.class).recoverWithUni(throwable -> mapToErrorRow(csvAggregate, throwable, aggregatesCsvResponse));
    }

    private Uni<Void> checkCsvAggregatePagoPaAndFillAggregateOrErrorList(Csv csv, AggregatesCsv aggregatesCsv) {
        CsvAggregatePagoPa csvAggregate = (CsvAggregatePagoPa) csv;

        return checkCsvAggregatePagoPa(csvAggregate)
                .onItem().invoke(() -> aggregatesCsv.getValidAggregates().add(csvAggregate))
                .onFailure(ResourceNotFoundException.class).recoverWithUni(throwable -> mapToErrorRow(csvAggregate, throwable, aggregatesCsv))
                .onFailure(InvalidRequestException.class).recoverWithUni(throwable -> mapToErrorRow(csvAggregate, throwable, aggregatesCsv));
    }

    private static RowError mapToAppIoErrorRow(CsvAggregateAppIo csvAggregateAppIo, Throwable throwable) {
        return new RowError(csvAggregateAppIo.getRowNumber(), csvAggregateAppIo.getTaxCode(), throwable.getMessage());
    }

    private static Uni<Void> mapToErrorRow(Csv csv, Throwable throwable, AggregatesCsv aggregatesCsv) {
        CsvAggregateAppIo csvAggregateAppIo = (CsvAggregateAppIo) csv;
        aggregatesCsv.getRowErrorList().add(new RowError(csvAggregateAppIo.getRowNumber(), csvAggregateAppIo.getTaxCode(), throwable.getMessage()));
        return Uni.createFrom().voidItem();
    }

    private Uni<AggregateAppIo> checkCsvAggregateAppIo(CsvAggregateAppIo csvAggregateAppIo) {
        return checkRequiredFieldsAppIo(csvAggregateAppIo)
                .onItem().transformToUni(unused -> retrieveDataFromIpa(csvAggregateAppIo));
    }

    private Uni<Void> checkCsvAggregateSend(CsvAggregateSend csvAggregate) {
        return checkRequiredFieldsSend(csvAggregate)
                .onItem().transformToUni(unused -> checkSubunitTypeSend(csvAggregate));
    }

    private Uni<Void> checkCsvAggregatePagoPa(CsvAggregatePagoPa csvAggregate) {
        return checkRequiredFieldsPagoPa(csvAggregate);
    }

    private Uni<AggregateAppIo> retrieveDataFromIpa(CsvAggregateAppIo csvAggregateAppIo) {
        AggregateAppIo aggregateAppIo = onboardingMapper.csvToAggregateAppIo(csvAggregateAppIo);
        aggregateAppIo.setOrigin(InstitutionResource.OriginEnum.IPA.value());

        if (StringUtils.isEmpty(aggregateAppIo.getSubunitType())) {
            return institutionApi.findInstitutionUsingGET(csvAggregateAppIo.getTaxCode(), null, null)
                    .onFailure(this::checkIfNotFound).recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA)))
                    .onItem().transformToUni(institutionResource -> retrieveCityCountyAndMapIpaFieldForPA(institutionResource, aggregateAppIo));
        } else if (InstitutionPaSubunitType.AOO.name().equals(aggregateAppIo.getSubunitType())) {
            return aooApi.findByUnicodeUsingGET(aggregateAppIo.getSubunitCode(), null)
                    .onFailure(this::checkIfNotFound)
                    .recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA)))
                    .onItem().transformToUni(aooResource -> retrieveCityCountyAndMapIpaFieldForAOO(aooResource, aggregateAppIo));
        } else if (UO.name().equals(aggregateAppIo.getSubunitType())) {
            return uoApi.findByUnicodeUsingGET1(aggregateAppIo.getSubunitCode(), null)
                    .onFailure(this::checkIfNotFound)
                    .recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA)))
                    .onItem().transformToUni(uoResource -> retrieveCityCountyAndMapIpaFieldForUO(uoResource, aggregateAppIo));
        } else {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_SUBUNIT_TYPE));
        }
    }

    private Uni<AggregateAppIo> retrieveCityCountyAndMapIpaFieldForUO(UOResource uoResource, AggregateAppIo aggregateAppIo) {
        return retrieveGeographicTaxonomies(uoResource.getCodiceComuneISTAT())
                .onItem().transformToUni(geographicTaxonomyResource -> {
                    mapIpaField(uoResource.getDenominazioneEnte(), uoResource.getIndirizzo(), uoResource.getCap(),null, aggregateAppIo, geographicTaxonomyResource);
                    return retrieveDigitalAddress(uoResource.getTipoMail1(), uoResource.getMail1(), uoResource.getCodiceFiscaleEnte(), aggregateAppIo);
                });
    }

    private Uni<AggregateAppIo> retrieveCityCountyAndMapIpaFieldForAOO(AOOResource aooResource, AggregateAppIo aggregateAppIo) {
        return retrieveGeographicTaxonomies(aooResource.getCodiceComuneISTAT())
                .onItem().transformToUni(geographicTaxonomyResource -> {
                    mapIpaField(aooResource.getDenominazioneEnte(), aooResource.getIndirizzo(), aooResource.getCap(), null, aggregateAppIo, geographicTaxonomyResource);
                    return retrieveDigitalAddress(aooResource.getTipoMail1(), aooResource.getMail1(), aooResource.getCodiceFiscaleEnte(), aggregateAppIo);
                });
    }

    private Uni<AggregateAppIo> retrieveCityCountyAndMapIpaFieldForPA(InstitutionResource institutionResource, AggregateAppIo aggregateAppIo) {
        return retrieveGeographicTaxonomies(institutionResource.getIstatCode())
                .onItem().transform(geographicTaxonomyResource -> {
                    mapIpaField(institutionResource.getDescription(), institutionResource.getAddress(), institutionResource.getZipCode(), institutionResource.getOriginId(), aggregateAppIo, geographicTaxonomyResource);aggregateAppIo.setDigitalAddress(institutionResource.getDigitalAddress());
                    return aggregateAppIo;
                });
    }

    private Uni<AggregateAppIo> retrieveDigitalAddress(String mailType, String mail, String taxCode, AggregateAppIo aggregateAppIo) {
        if (Objects.equals(mailType, PEC)) {
            aggregateAppIo.setDigitalAddress(mail);
            return Uni.createFrom().item(aggregateAppIo);
        } else {
            return institutionApi.findInstitutionUsingGET(taxCode, null, null)
                    .onItem().invoke(institutionResource -> aggregateAppIo.setDigitalAddress(institutionResource.getDigitalAddress()))
                    .replaceWith(aggregateAppIo);
        }
    }

    private static void mapIpaField(String description, String address, String zipCode, String originId, AggregateAppIo aggregateAppIo, GeographicTaxonomyFromIstatCode geographicTaxonomyFromIstatCode) {
        aggregateAppIo.setCounty(geographicTaxonomyFromIstatCode.getCounty());
        aggregateAppIo.setCity(geographicTaxonomyFromIstatCode.getCity());
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

    private Uni<Void> checkSubunitTypeSend(CsvAggregateSend csvAggregate) {

        if (StringUtils.isEmpty(csvAggregate.getSubunitType())) {
            return institutionApi.findInstitutionUsingGET(csvAggregate.getTaxCode(), null, null)
                    .onFailure(this::checkIfNotFound).recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA)))
                    .replaceWith(Uni.createFrom().voidItem());
        } else if (InstitutionPaSubunitType.AOO.name().equals(csvAggregate.getSubunitType())) {
            return aooApi.findByUnicodeUsingGET(csvAggregate.getSubunitCode(), null)
                    .onFailure(this::checkIfNotFound).recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA)))
                    .replaceWith(Uni.createFrom().voidItem());
        } else if (InstitutionPaSubunitType.UO.name().equals(csvAggregate.getSubunitType())) {
            return uoApi.findByUnicodeUsingGET1(csvAggregate.getSubunitCode(), null)
                    .onFailure(this::checkIfNotFound).recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA)))
                    .replaceWith(Uni.createFrom().voidItem());
        } else {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_SUBUNIT_TYPE));
        }
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

    private Uni<Void> checkRequiredFieldsSend(CsvAggregateSend csvAggregate) {

        if (StringUtils.isEmpty(csvAggregate.getDescription())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_DESCRIPTION));
        } else if (StringUtils.isEmpty(csvAggregate.getPec())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_PEC));
        }
        if (StringUtils.isEmpty(csvAggregate.getTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE));
        } else if (StringUtils.isEmpty(csvAggregate.getVatNumber())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_VATNUMBER));
        } else if (StringUtils.isEmpty(csvAggregate.getAddress())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADDRESS));
        } else if (StringUtils.isEmpty(csvAggregate.getCity())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_CITY));
        } else if (StringUtils.isEmpty(csvAggregate.getProvince())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_PROVINCE));
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
        } else if ((StringUtils.isEmpty(csvAggregate.getSubunitType()) && StringUtils.isEmpty(csvAggregate.getSubunitCode()))
                && (StringUtils.isEmpty(csvAggregate.getIpaCode()))) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_IPA_CODE));
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> checkRequiredFieldsPagoPa(CsvAggregatePagoPa csvAggregate) {

        if (StringUtils.isEmpty(csvAggregate.getDescription())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_DESCRIPTION));
        } else if (StringUtils.isEmpty(csvAggregate.getPec())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_PEC));
        }
        if (StringUtils.isEmpty(csvAggregate.getTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE));
        } else if (StringUtils.isEmpty(csvAggregate.getVatNumber())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_VATNUMBER));
        } else if (StringUtils.isEmpty(csvAggregate.getAddress())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADDRESS));
        } else if (StringUtils.isEmpty(csvAggregate.getCity())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_CITY));
        } else if (StringUtils.isEmpty(csvAggregate.getProvince())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_PROVINCE));
        } else if (StringUtils.isEmpty(csvAggregate.getAggragateNamePT())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_AGGREGATE_NAME_PT));
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

    public <T extends Csv> AggregatesCsv<T> readItemsFromCsv(File file, Class<T> csv) {
        List<Csv> resultList = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            StringReader stringReader = new StringReader(new String(fileBytes, StandardCharsets.UTF_8));
            BufferedReader bufferedReader = new BufferedReader(stringReader);
            String skip = bufferedReader.readLine();
            log.info("Skip header: " + skip);
            int lineNumber = 1;
            String nextLine;

            while ((nextLine = bufferedReader.readLine()) != null) {
                if (!nextLine.startsWith("(*")) {
                    parseLine(nextLine, lineNumber, resultList, errors, csv);
                    lineNumber++;
                }
            }

            return new AggregatesCsv(resultList, errors);

        } catch (Exception e) {
            log.error(ERROR_READING_CSV + e.getMessage(), e);
            throw new InvalidRequestException(ERROR_READING_CSV + e.getMessage());
        }
    }

    private <T extends Csv> void parseLine(String nextLine, int lineNumber, List<Csv> resultList, List<RowError> errors, Class<T> csv) {
        try {
            StringReader lineReader = new StringReader(nextLine);
            CsvToBean<Csv> csvToBean = getAggregateCsvToBean(new BufferedReader(lineReader), csv);
            List<Csv> csvAggregateList = csvToBean.parse();
            if (!csvAggregateList.isEmpty()) {
                Csv csvAggregate = csvAggregateList.get(0);
                csvAggregate.setRowNumber(lineNumber);
                resultList.add(csvAggregateList.get(0));
            }
            log.debug("Row " + lineNumber + ": ");
        } catch (Exception e) {
            log.error("Error to the row " + lineNumber + ": " + e.getMessage());
            errors.add(new RowError(lineNumber, "", MALFORMED_ROW));
        }
    }

    private <T extends Csv> CsvToBean<Csv> getAggregateCsvToBean(BufferedReader bufferedReader, Class<T> csv) {
        CsvToBeanBuilder<Csv> csvToBeanBuilder = new CsvToBeanBuilder<>(bufferedReader);
        csvToBeanBuilder.withType(csv);
        csvToBeanBuilder.withSeparator(';');
        csvToBeanBuilder.withQuoteChar(DEFAULT_QUOTE_CHARACTER);
        csvToBeanBuilder.withOrderedResults(true);
        csvToBeanBuilder.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS);
        csvToBeanBuilder.withThrowExceptions(false);
        return csvToBeanBuilder.build();
    }
}
