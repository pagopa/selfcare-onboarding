package it.pagopa.selfcare.service.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;
import java.util.*;

/**
 * It validate the onboarding request based on an allowed-list loaded from a property.
 */
@Slf4j
@ApplicationScoped
public class ConfigMapAllowedListOnboardingValidationStrategy implements OnboardingValidationStrategy {

    /**
     * It represent, if present, the institutions and products allowed to be onboarded (i.e. an allowed-list).
     * The {@code Map} has as key the product id  and as values a list of institution external id allowed for that product
     * A {@code *} value means "anything".
     * If used, the '*' is the only value allowed for a given key.
     */
    private final Optional<Map<String, Set<String>>> institutionProductsAllowedMap;


    public ConfigMapAllowedListOnboardingValidationStrategy(@ConfigProperty(name = "onboarding.institutions-allowed-list") Optional<String> institutionProductsAllowedString) throws JsonProcessingException {
        log.trace("Initializing {}", ConfigMapAllowedListOnboardingValidationStrategy.class.getSimpleName());
        log.debug("ConfigMapAllowedListOnboardingValidationStrategy institutionProductsAllowedMap = {}", institutionProductsAllowedString);
        HashMap<String, Set<String>> institutionProductsAllowedMap = null;

        if(institutionProductsAllowedString.isPresent()) {
            TypeReference<HashMap<String, Set<String>>> typeRef = new TypeReference<HashMap<String, Set<String>>>() {};
            institutionProductsAllowedMap = new ObjectMapper().readValue(institutionProductsAllowedString.get().replace("'","\""), typeRef);
            validateSpecialcharecterUsage(institutionProductsAllowedMap);
        }
        this.institutionProductsAllowedMap = Optional.ofNullable(institutionProductsAllowedMap);
    }


    private void validateSpecialcharecterUsage(Map<String, Set<String>> allowedList) {
        if (allowedList != null) {
            allowedList.forEach((productId, institutionExternalIds) -> {
                if (institutionExternalIds.size() > 1
                        && institutionExternalIds.stream().anyMatch("*"::equals)) {
                    throw new IllegalArgumentException(String.format("Invalid configuration: bad using of special character '*' in allowed-list for key '%s'. If used, the '*' is the only value allowed for a given key",
                            productId));
                }
            });
        }
    }


    /**
     * If the allowed-list is present and the provided {@code productId} and {@code institutionExternalId} are not in that list, an execption is thrown.
     * Otherwise, if the allowed-is is not present, then no validation is applied.
     *
     * @param productId             the product id
     * @param institutionExternalId the institution external id
     */
    @Override
    public boolean validate(String productId, String institutionExternalId) {
        log.trace("validate start");
        log.debug("validate productId = {}, institutionExternalId = {}", productId, institutionExternalId);
        final boolean valid = institutionProductsAllowedMap.isEmpty() ||
                Optional.ofNullable(institutionProductsAllowedMap.get().get(productId))
                        .map(institutionExternalIds -> institutionExternalIds.contains("*")
                                || institutionExternalIds.contains(institutionExternalId))
                        .orElse(false);
        log.debug("validate result = {}", valid);
        log.trace("validate end");
        return valid;
    }

    public static void main(String[] args) throws JsonProcessingException {
        String prova = "{\"prod-interop\":[\"*\"],\"prod-pn\":[\"*\"],\"prod-io\":[\"*\"]}";
        TypeReference<HashMap<String,Set<String>>> typeRef
                = new TypeReference<HashMap<String,Set<String>>>() {};

        HashMap<String,Set<String>> map = new ObjectMapper().readValue(prova, typeRef);
        System.out.println(map);
    }

}
