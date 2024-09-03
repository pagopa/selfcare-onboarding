package it.pagopa.selfcare.onboarding.model;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class CsvAggregateAppIo implements Csv {

    @CsvBindByPosition(position = 0)
    private String aggregateName;

    @CsvBindByPosition(position = 1)
    private String pec;

    @CsvBindByPosition(position = 2)
    private String taxCode;

    @CsvBindByPosition(position = 3)
    private String vatNumber;

    @CsvBindByPosition(position = 4)
    private String address;

    @CsvBindByPosition(position = 5)
    private String city;

    @CsvBindByPosition(position = 6)
    private String province;

    @CsvBindByPosition(position = 7)
    private String ipaCode;

    @CsvBindByPosition(position = 8)
    private String subunitType;

    @CsvBindByPosition(position = 9)
    private String subunitCode;

    @CsvBindByPosition(position = 10)
    private String originId;
    private Integer rowNumber;

    @Override
    public void setRowNumber(int lineNumber) {
        this.rowNumber = lineNumber;
    }

}
