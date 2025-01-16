package it.pagopa.selfcare.onboarding.util;

import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.model.FormItem;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.multipart.FormValue;

import java.io.File;
import java.util.Deque;
import java.util.function.BinaryOperator;

@ApplicationScoped
public class Utils {
    private static final String DEFAULT_CONTRACT_FORM_DATA_NAME = "contract";

    private Utils() {
    }

    public static FormItem retrieveContractFromFormData(FormData formData, File file) {
        Deque<FormValue> deck = formData.get(DEFAULT_CONTRACT_FORM_DATA_NAME);
        if (deck.size() > 1) {
            throw new InvalidRequestException(
                    CustomError.TOO_MANY_CONTRACTS.getMessage(), CustomError.TOO_MANY_CONTRACTS.getCode());
        }

        return FormItem.builder().file(file).fileName(deck.getFirst().getFileName()).build();
    }

    public static final BinaryOperator<String> CONTRACT_FILENAME_FUNC =
            (filename, productName) ->
                    String.format(filename, StringUtils.stripAccents(productName.replaceAll("\\s+", "_")));



    public String extractSurnamePart(String surname) {
        String consonants = surname.replaceAll("[AEIOUaeiou]", "");
        String vowels = surname.replaceAll("[^AEIOUaeiou]", "");
        String part = (consonants + vowels).toUpperCase();
        return part.length() >= 3 ? part.substring(0, 3) : padWithX(part);
    }

    public String extractNamePart(String name) {
        String consonants = name.replaceAll("[AEIOUaeiou]", "");
        String vowels = name.replaceAll("[^AEIOUaeiou]", "");
        if (consonants.length() > 3) {
            consonants = String.format("%s%s%s", consonants.charAt(0), consonants.charAt(2), consonants.charAt(3));
        }
        String part = (consonants + vowels).toUpperCase();
        return part.length() >= 3 ? part.substring(0, 3) : padWithX(part);
    }

    private String padWithX(String input) {
        StringBuilder sb = new StringBuilder(input);
        while (sb.length() < 3) {
            sb.append("X");
        }
        return sb.toString();
    }


}
