package it.pagopa.selfcare.onboarding.model;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class CsvAggregateSend implements Csv {

    @CsvBindByPosition(position = 0)
    private String aggregateName;

    @CsvBindByPosition(position = 1)
    private String pec;

    @CsvBindByPosition(position = 2)
    private String taxCode;

    @CsvBindByPosition(position = 3)
    private String vatNumber;

    @CsvBindByPosition(position = 4)
    private String codeSDI;

    @CsvBindByPosition(position = 5)
    private String address;

    @CsvBindByPosition(position = 6)
    private String city;

    @CsvBindByPosition(position = 7)
    private String province;

    @CsvBindByPosition(position = 8)
    private String ipaCode;

    @CsvBindByPosition(position = 9)
    private String subunitType;

    @CsvBindByPosition(position = 10)
    private String subunitCode;

    @CsvBindByPosition(position = 11)
    private String adminAgreggateName;

    @CsvBindByPosition(position = 12)
    private String adminAgreggateSurname;

    @CsvBindByPosition(position = 13)
    private String adminAgreggateTaxCode;

    @CsvBindByPosition(position = 14)
    private String adminAgreggateEmail;
    private Integer rowNumber;
    @Override
    public void setRowNumber(int lineNumber) {
        this.rowNumber = lineNumber;
    }

}
