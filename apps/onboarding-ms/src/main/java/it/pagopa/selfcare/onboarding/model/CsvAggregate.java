package it.pagopa.selfcare.onboarding.model;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class CsvAggregate {

    @CsvBindByPosition(position = 0)
    private String taxCode;
    @CsvBindByPosition(position = 1)
    private String description;
    @CsvBindByPosition(position = 2)
    private String vatNumber;
    @CsvBindByPosition(position = 3)
    private String subunitType;
    @CsvBindByPosition(position = 4)
    private String subunitCode;
    @CsvBindByPosition(position = 5)
    private String address;
    @CsvBindByPosition(position = 6)
    private String originId;

    private Integer rowNumber;


}
