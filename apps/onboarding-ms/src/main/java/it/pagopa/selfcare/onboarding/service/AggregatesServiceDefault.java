package it.pagopa.selfcare.onboarding.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.AggregatesCsvResponse;
import it.pagopa.selfcare.onboarding.model.CsvAggregate;
import it.pagopa.selfcare.onboarding.model.RowError;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateResponse;
import it.pagopa.selfcare.onboarding.util.InstitutionPaSubunitType;
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

    @Override
    public Uni<VerifyAggregateResponse> validateAggregatesCsv(File file){
        AggregatesCsvResponse aggregatesCsvResponse = readItemsFromCsv(file);
        List<CsvAggregate> csvAggregates = aggregatesCsvResponse.getCsvAggregateList();
        return Multi.createFrom().iterable(csvAggregates)
                .onItem().transformToUniAndMerge(csvAggregate -> checkCsvAggregateAndFillAggregateOrErrorList(csvAggregate, aggregatesCsvResponse))
                .collect().asList()
                .onItem().transform(list -> onboardingMapper.toVerifyAggregateResponse(aggregatesCsvResponse))
                .onItem().invoke(() -> LOG.infof("CSV file validated end: %s valid row and %s invalid row",
                        aggregatesCsvResponse.getValidAggregates().size(),
                        aggregatesCsvResponse.getRowErrorList().size()));
    }

    private Uni<Void> checkCsvAggregateAndFillAggregateOrErrorList(CsvAggregate csvAggregate, AggregatesCsvResponse aggregatesCsvResponse) {
        return checkCsvAggregate(csvAggregate)
                .onItem().invoke(() -> aggregatesCsvResponse.getValidAggregates().add(csvAggregate))
                .onFailure(ResourceNotFoundException.class).recoverWithUni(throwable -> mapToErrorRow(csvAggregate, throwable, aggregatesCsvResponse))
                .onFailure(InvalidRequestException.class).recoverWithUni(throwable -> mapToErrorRow(csvAggregate, throwable, aggregatesCsvResponse));
    }

    private static Uni<Void> mapToErrorRow(CsvAggregate csvAggregate, Throwable throwable, AggregatesCsvResponse aggregatesCsvResponse) {
        aggregatesCsvResponse.getRowErrorList().add(new RowError(csvAggregate.getRowNumber(), csvAggregate.getTaxCode(), throwable.getMessage()));
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> checkCsvAggregate(CsvAggregate csvAggregate) {
        return checkRequiredFields(csvAggregate)
                .onItem().transformToUni(unused -> checkSubunitType(csvAggregate));
    }

    private Uni<Void> checkSubunitType(CsvAggregate csvAggregate) {
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

    private Uni<Void> checkRequiredFields(CsvAggregate csvAggregate) {
        if (StringUtils.isEmpty(csvAggregate.getTaxCode())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_TAXCODE));
        } else if (StringUtils.isEmpty(csvAggregate.getDescription())) {
            return Uni.createFrom().failure(new InvalidRequestException(ERROR_DESCRIPTION));
        }
        return Uni.createFrom().voidItem();
    }

    public AggregatesCsvResponse readItemsFromCsv(File file) {
        List<CsvAggregate> resultList = new ArrayList<>();
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
                parseLine(nextLine, lineNumber, resultList, errors);
                lineNumber++;
            }

            return new AggregatesCsvResponse(resultList, errors);

        } catch (Exception e) {
            log.error(ERROR_READING_CSV + e.getMessage(), e);
            throw new InvalidRequestException(ERROR_READING_CSV + e.getMessage());
        }
    }

    private void parseLine(String nextLine, int lineNumber, List<CsvAggregate> resultList, List<RowError> errors) {
        try {
            StringReader lineReader = new StringReader(nextLine);
            CsvToBean<CsvAggregate> csvToBean = getAggregateCsvToBean(new BufferedReader(lineReader));
            List<CsvAggregate> csvAggregateList = csvToBean.parse();
            if (!csvAggregateList.isEmpty()) {
                CsvAggregate csvAggregate = csvAggregateList.get(0);
                csvAggregate.setRowNumber(lineNumber);
                resultList.add(csvAggregateList.get(0));
            }
            log.debug("Row " + lineNumber + ": ");
        } catch (Exception e) {
            log.error("Error to the row " + lineNumber + ": " + e.getMessage());
            errors.add(new RowError(lineNumber, "", MALFORMED_ROW));
        }
    }

    private CsvToBean<CsvAggregate> getAggregateCsvToBean(BufferedReader bufferedReader) {
        CsvToBeanBuilder<CsvAggregate> csvToBeanBuilder = new CsvToBeanBuilder<>(bufferedReader);
        csvToBeanBuilder.withType(CsvAggregate.class);
        csvToBeanBuilder.withSeparator(';');
        csvToBeanBuilder.withQuoteChar(DEFAULT_QUOTE_CHARACTER);
        csvToBeanBuilder.withOrderedResults(true);
        csvToBeanBuilder.withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS);
        csvToBeanBuilder.withThrowExceptions(false);
        return csvToBeanBuilder.build();
    }
}
