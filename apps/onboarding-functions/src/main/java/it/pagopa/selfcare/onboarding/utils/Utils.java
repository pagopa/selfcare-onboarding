package it.pagopa.selfcare.onboarding.utils;

import java.util.function.Function;

public class Utils {

    public static final String PDF_FORMAT_FILENAME = "%s_accordo_adesione.pdf";

    public static final Function<String, String> CONTRACT_FILENAME_FUNC =
            productName -> String.format(PDF_FORMAT_FILENAME, productName.replaceAll("\\s+","_"));
}
