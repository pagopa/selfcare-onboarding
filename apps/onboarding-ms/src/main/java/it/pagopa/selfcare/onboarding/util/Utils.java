package it.pagopa.selfcare.onboarding.util;

import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.model.FormItem;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.multipart.FormValue;

import java.io.File;
import java.util.Deque;
import java.util.function.BinaryOperator;

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


}
