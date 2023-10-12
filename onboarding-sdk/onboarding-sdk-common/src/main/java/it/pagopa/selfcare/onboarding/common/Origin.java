package it.pagopa.selfcare.onboarding.common;

import org.springframework.util.StringUtils;

import java.util.Arrays;

public enum Origin {
    MOCK("MOCK"),
    IPA("IPA"),
    SELC("SELC"),
    ANAC("ANAC"),
    UNKNOWN("UNKNOWN"),
    ADE("ADE"),
    INFOCAMERE("INFOCAMERE");

    private final String value;

    Origin(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }


    public static Origin fromValue(String value) {
        if(StringUtils.hasText(value)) {
            return Arrays.stream(values())
                    .filter(origin -> origin.toString().equals(value))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Valid value for Origin are: IPA, INFOCAMERE, SELC or static"));
        }else{
            return Origin.UNKNOWN;
        }
    }
    
}