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
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.opencsv.ICSVParser.DEFAULT_QUOTE_CHARACTER;

@ApplicationScoped
@Slf4j
public class AggregatesServiceDefault implements AggregatesService{

    private static final Logger LOG = Logger.getLogger(AggregatesServiceDefault.class);


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


    @Override
    public Uni<VerifyAggregateResponse> validateAppIoAggregatesCsv(File file){
        AggregatesCsvResponse aggregatesCsvResponse = readItemsFromCsv(file, CsvAggregateAppIo.class);
        List<Csv> csvAggregates = aggregatesCsvResponse.getCsvAggregateList();
        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregate -> checkCsvAggregateAppIoAndFillAggregateOrErrorList(csvAggregate, aggregatesCsvResponse))
                .collect().asList()
                .onItem().transform(list -> onboardingMapper.toVerifyAggregateResponse(aggregatesCsvResponse))
                .onItem().invoke(() -> LOG.infof("CSV file validated end: %s valid row and %s invalid row",
                        aggregatesCsvResponse.getValidAggregates().size(),
                        aggregatesCsvResponse.getRowErrorList().size()));
    }

    @Override
    public Uni<VerifyAggregateSendResponse> validateSendAggregatesCsv(File file) {
        AggregatesCsvResponse aggregatesCsvResponse = readItemsFromCsv(file, CsvAggregateSend.class);
        List<Csv> csvAggregates = aggregatesCsvResponse.getCsvAggregateList();
        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregate -> checkCsvAggregateSendAndFillAggregateOrErrorList(csvAggregate, aggregatesCsvResponse))
                .collect().asList()
                .onItem().transform(list -> onboardingMapper.toVerifyAggregateSendResponse(aggregatesCsvResponse))
                .onItem().invoke(() -> LOG.infof("CSV file validated end: %s valid row and %s invalid row",
                        aggregatesCsvResponse.getValidAggregates().size(),
                        aggregatesCsvResponse.getRowErrorList().size()));
    }

    @Override
    public Uni<VerifyAggregateResponse> validatePagoPaAggregatesCsv(File file) {
        AggregatesCsvResponse aggregatesCsvResponse = readItemsFromCsv(file, CsvAggregatePagoPa.class);
        List<Csv> csvAggregates = aggregatesCsvResponse.getCsvAggregateList();
        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregate -> checkCsvAggregatePagoPaAndFillAggregateOrErrorList(csvAggregate, aggregatesCsvResponse))
                .collect().asList()
                .onItem().transform(list -> onboardingMapper.toVerifyAggregateResponse(aggregatesCsvResponse))
                .onItem().invoke(() -> LOG.infof("CSV file validated end: %s valid row and %s invalid row",
                        aggregatesCsvResponse.getValidAggregates().size(),
                        aggregatesCsvResponse.getRowErrorList().size()));

    }

    private Uni<Void> checkCsvAggregateAppIoAndFillAggregateOrErrorList(Csv csv, AggregatesCsvResponse aggregatesCsvResponse) {
        CsvAggregateAppIo csvAggregateAppIo = (CsvAggregateAppIo) csv;
        return checkCsvAggregateAppIo(csvAggregateAppIo)
                .onItem().invoke(() -> aggregatesCsvResponse.getValidAggregates().add(csvAggregateAppIo))
                .onFailure(ResourceNotFoundException.class).recoverWithUni(throwable -> mapToErrorRow(csvAggregateAppIo, throwable, aggregatesCsvResponse))
                .onFailure(InvalidRequestException.class).recoverWithUni(throwable -> mapToErrorRow(csvAggregateAppIo, throwable, aggregatesCsvResponse));
    }

    private Uni<Void> checkCsvAggregateSendAndFillAggregateOrErrorList(Csv csv, AggregatesCsvResponse aggregatesCsvResponse) {
        CsvAggregateSend csvAggregate = (CsvAggregateSend) csv;
        return checkCsvAggregateSend(csvAggregate)
                .onItem().invoke(() -> aggregatesCsvResponse.getValidAggregates().add(csvAggregate))
                .onFailure(ResourceNotFoundException.class).recoverWithUni(throwable -> mapToErrorRow(csvAggregate, throwable, aggregatesCsvResponse))
                .onFailure(InvalidRequestException.class).recoverWithUni(throwable -> mapToErrorRow(csvAggregate, throwable, aggregatesCsvResponse));
    }

    private Uni<Void> checkCsvAggregatePagoPaAndFillAggregateOrErrorList(Csv csv, AggregatesCsvResponse aggregatesCsvResponse) {
        CsvAggregatePagoPa csvAggregated = null;
        try {
            csvAggregated = (CsvAggregatePagoPa) csv;
        } catch (Exception ex){
            ex.printStackTrace();
            System.out.println("Error RR: " );
        }

        CsvAggregatePagoPa csvAggregate = csvAggregated;

        return checkCsvAggregatePagoPa(csvAggregate)
                .onItem().invoke(() -> aggregatesCsvResponse.getValidAggregates().add(csvAggregate))
                .onFailure(ResourceNotFoundException.class).recoverWithUni(throwable -> mapToErrorRow(csvAggregate, throwable, aggregatesCsvResponse))
                .onFailure(InvalidRequestException.class).recoverWithUni(throwable -> mapToErrorRow(csvAggregate, throwable, aggregatesCsvResponse));
    }

    private static Uni<Void> mapToErrorRow(Csv csv, Throwable throwable, AggregatesCsvResponse aggregatesCsvResponse) {
        CsvAggregateAppIo csvAggregateAppIo = (CsvAggregateAppIo) csv;
        aggregatesCsvResponse.getRowErrorList().add(new RowError(csvAggregateAppIo.getRowNumber(), csvAggregateAppIo.getTaxCode(), throwable.getMessage()));
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> checkCsvAggregateAppIo(Csv csvAggregate) {
        return checkRequiredFieldsAppIo(csvAggregate)
                .onItem().transformToUni(unused -> checkSubunitTypeAppIo(csvAggregate));
    }

    private Uni<Void> checkCsvAggregateSend(CsvAggregateSend csvAggregate) {
        return checkRequiredFieldsSend(csvAggregate)
                .onItem().transformToUni(unused -> checkSubunitTypeSend(csvAggregate));
    }

    private Uni<Void> checkCsvAggregatePagoPa(CsvAggregatePagoPa csvAggregate) {
        return checkRequiredFieldsPagoPa(csvAggregate);
    }

    private Uni<Void> checkSubunitTypeAppIo(Csv csv) {
        CsvAggregateAppIo csvAggregateAppIo = (CsvAggregateAppIo) csv;

        if (StringUtils.isEmpty(csvAggregateAppIo.getSubunitType())) {
            return institutionApi.findInstitutionUsingGET(csvAggregateAppIo.getTaxCode(), null, null)
                    .onFailure(this::checkIfNotFound).recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA)))
                    .replaceWith(Uni.createFrom().voidItem());
        } else if (InstitutionPaSubunitType.AOO.name().equals(csvAggregateAppIo.getSubunitType())) {
            return aooApi.findByUnicodeUsingGET(csvAggregateAppIo.getSubunitCode(), null)
                    .onFailure(this::checkIfNotFound).recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA)))
                    .replaceWith(Uni.createFrom().voidItem());
        } else if (InstitutionPaSubunitType.UO.name().equals(csvAggregateAppIo.getSubunitType())) {
            return uoApi.findByUnicodeUsingGET1(csvAggregateAppIo.getSubunitCode(), null)
                    .onFailure(this::checkIfNotFound).recoverWithUni(Uni.createFrom().failure(new ResourceNotFoundException(ERROR_IPA)))
                    .replaceWith(Uni.createFrom().voidItem());
        } else {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_SUBUNIT_TYPE));
        }
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

    private Uni<Void> checkRequiredFieldsAppIo(Csv csv) {
        CsvAggregateAppIo csvAggregateAppIo = (CsvAggregateAppIo) csv;

        if (StringUtils.isEmpty(csvAggregateAppIo.getDescription())){
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_DESCRIPTION));
        }
        else if (StringUtils.isEmpty(csvAggregateAppIo.getPec())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_PEC));
        }
        else if (StringUtils.isEmpty(csvAggregateAppIo.getTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE));
        }
        else if (StringUtils.isEmpty(csvAggregateAppIo.getVatNumber())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_VATNUMBER));
        }
        else if (StringUtils.isEmpty(csvAggregateAppIo.getAddress())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADDRESS));
        }
        else if (StringUtils.isEmpty(csvAggregateAppIo.getCity())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_CITY));
        }
        else if (StringUtils.isEmpty(csvAggregateAppIo.getProvince())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_PROVINCE));
        }
        else if ((StringUtils.isEmpty(csvAggregateAppIo.getSubunitType()) && StringUtils.isNotEmpty(csvAggregateAppIo.getSubunitCode()))
                || (StringUtils.isNotEmpty(csvAggregateAppIo.getSubunitType()) && StringUtils.isEmpty(csvAggregateAppIo.getSubunitCode()))) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_AOO_UO));
        }
        else if ((StringUtils.isEmpty(csvAggregateAppIo.getSubunitType()) && StringUtils.isEmpty(csvAggregateAppIo.getSubunitCode()))
                && (StringUtils.isEmpty(csvAggregateAppIo.getIpaCode()))) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_IPA_CODE));
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> checkRequiredFieldsSend(CsvAggregateSend csvAggregate) {

        if (StringUtils.isEmpty(csvAggregate.getDescription())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_DESCRIPTION));
        }
        else if (StringUtils.isEmpty(csvAggregate.getPec())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_PEC));
        }
        if (StringUtils.isEmpty(csvAggregate.getTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE));
        }
        else if (StringUtils.isEmpty(csvAggregate.getVatNumber())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_VATNUMBER));
        }
        else if (StringUtils.isEmpty(csvAggregate.getAddress())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADDRESS));
        }
        else if (StringUtils.isEmpty(csvAggregate.getCity())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_CITY));
        }
        else if (StringUtils.isEmpty(csvAggregate.getProvince())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_PROVINCE));
        }
        else if ((StringUtils.isEmpty(csvAggregate.getSubunitType()) && StringUtils.isNotEmpty(csvAggregate.getSubunitCode()))
                || (StringUtils.isNotEmpty(csvAggregate.getSubunitType()) && StringUtils.isEmpty(csvAggregate.getSubunitCode()))) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_AOO_UO));
        }
        else if (StringUtils.isEmpty(csvAggregate.getAdminAggregateName())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_NAME));
        }
        else if (StringUtils.isEmpty(csvAggregate.getAdminAggregateSurname())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_SURNAME));
        }
        else if (StringUtils.isEmpty(csvAggregate.getAdminAggregateTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_TAXCODE));
        }
        else if (StringUtils.isEmpty(csvAggregate.getAdminAggregateEmail())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADMIN_EMAIL));
        }
        else if ((StringUtils.isEmpty(csvAggregate.getSubunitType()) && StringUtils.isEmpty(csvAggregate.getSubunitCode()))
                && (StringUtils.isEmpty(csvAggregate.getIpaCode()))) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_IPA_CODE));
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> checkRequiredFieldsPagoPa(CsvAggregatePagoPa csvAggregate) {

        if (StringUtils.isEmpty(csvAggregate.getDescription())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_DESCRIPTION));
        }
        else if (StringUtils.isEmpty(csvAggregate.getPec())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_PEC));
        }
        if (StringUtils.isEmpty(csvAggregate.getTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE));
        }
        else if (StringUtils.isEmpty(csvAggregate.getVatNumber())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_VATNUMBER));
        }
        else if (StringUtils.isEmpty(csvAggregate.getAddress())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_ADDRESS));
        }
        else if (StringUtils.isEmpty(csvAggregate.getCity())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_CITY));
        }
        else if (StringUtils.isEmpty(csvAggregate.getProvince())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_PROVINCE));
        }
        else if (StringUtils.isEmpty(csvAggregate.getAggragateNamePT())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_AGGREGATE_NAME_PT));
        }
        else if (StringUtils.isEmpty(csvAggregate.getTaxCodePT())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE_PT));
        }
        else if (StringUtils.isEmpty(csvAggregate.getIban())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_IBAN));
        }
        else if (StringUtils.isEmpty(csvAggregate.getService())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_SERVICE));
        }
        else if (StringUtils.isEmpty(csvAggregate.getSyncAsyncMode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_SYNC_ASYNC_MODE));
        }
        return Uni.createFrom().voidItem();
    }

    public <T extends Csv> AggregatesCsvResponse readItemsFromCsv(File file, Class<T> csv) {
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
                if(!nextLine.startsWith("(*")){
                    parseLine(nextLine, lineNumber, resultList, errors, csv);
                    lineNumber++;
                }
            }

            return new AggregatesCsvResponse(resultList, errors);

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
