package it.pagopa.selfcare.onboarding.model;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class CsvAggregatePagoPa implements Csv {

    @CsvBindByPosition(position = 0)
    private String aggregateName;

    @CsvBindByPosition(position = 1)
    private String emailPec;

    @CsvBindByPosition(position = 2)
    private String taxNumber;

    @CsvBindByPosition(position = 3)
    private String vatNumber;

    @CsvBindByPosition(position = 4)
    private String address;

    @CsvBindByPosition(position = 5)
    private String taxCodePT;

    @CsvBindByPosition(position = 6)
    private String vatNumberPT;

    @CsvBindByPosition(position = 7)
    private String iban;

    @CsvBindByPosition(position = 8)
    private String service;

    @CsvBindByPosition(position = 9)
    private String syncAsyncMode;

    private Integer rowNumber;

    @Override
    public void setRowNumber(int lineNumber) {
        this.rowNumber = lineNumber;
    }
}
