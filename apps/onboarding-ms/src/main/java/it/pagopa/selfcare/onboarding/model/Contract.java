package it.pagopa.selfcare.onboarding.model;

import lombok.Builder;
import lombok.Getter;

import java.io.File;

@Builder
@Getter
public class Contract {
    private File file;
    private String fileName;
}
