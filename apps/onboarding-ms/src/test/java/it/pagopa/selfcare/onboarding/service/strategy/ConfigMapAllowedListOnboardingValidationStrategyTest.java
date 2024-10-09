package it.pagopa.selfcare.onboarding.service.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ConfigMapAllowedListOnboardingValidationStrategyTest {

    @Test
    void validate_allowedListNotConfigured() throws JsonProcessingException {
        // given

        final String productId = "prod-io";
        final String institutionExternalId = "inst-1";
        final ConfigMapAllowedListOnboardingValidationStrategy validationStrategy =
                new ConfigMapAllowedListOnboardingValidationStrategy(Optional.empty());
        // when
        final Executable executable = () -> validationStrategy.validate(productId, institutionExternalId);
        // then
        assertDoesNotThrow(executable);
    }


    @Test
    void validate_productNotInConfig() throws JsonProcessingException {
        // given
        final String productId = "prod-io";
        final String institutionExternalId = "inst-1";
        final ConfigMapAllowedListOnboardingValidationStrategy validationStrategy =
                new ConfigMapAllowedListOnboardingValidationStrategy(Optional.of("{}"));
        // when
        final boolean validate = validationStrategy.validate(productId, institutionExternalId);
        // then
        assertFalse(validate);
    }


    @Test
    void validate_invalidConfig_invalidUsageOfSpecialCharacter() {
        // given
        // when
        final Executable executable = () -> new ConfigMapAllowedListOnboardingValidationStrategy(Optional.of("{'prod-io':['inst-1','*']}"));
        // then
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Invalid configuration: bad using of special character '*' in allowed-list for key 'prod-io'. If used, the '*' is the only value allowed for a given key", e.getMessage());
    }

    @Test
    void validate_allowedListSizeGreaterThanOne() throws JsonProcessingException {
        // given
        final String productId = "prod-io";
        final String institutionExternalId = "inst-2";
        final ConfigMapAllowedListOnboardingValidationStrategy validationStrategy =
                new ConfigMapAllowedListOnboardingValidationStrategy(Optional.of("{'prod-io':['inst-1','inst-2','inst-3']}"));
        // when
        final boolean validate = validationStrategy.validate(productId, institutionExternalId);
        // then
        assertTrue(validate);
    }


    @Test
    void validate_institutionExplicitlyInAllowed() throws JsonProcessingException {
        // given
        final String productId = "prod-io";
        final String institutionExternalId = "inst-1";
        final ConfigMapAllowedListOnboardingValidationStrategy validationStrategy =
                new ConfigMapAllowedListOnboardingValidationStrategy(Optional.of("{'prod-io':['inst-1']}"));
        // when
        final boolean validate = validationStrategy.validate(productId, institutionExternalId);
        // then
        assertTrue(validate);
    }


    @Test
    void validate_institutionImplicitlyInAllowedList() throws JsonProcessingException {
        // given
        final String productId = "prod-io";
        final String institutionExternalId = "inst-1";
        final ConfigMapAllowedListOnboardingValidationStrategy validationStrategy =
                new ConfigMapAllowedListOnboardingValidationStrategy(Optional.empty());
        // when
        final boolean validate = validationStrategy.validate(productId, institutionExternalId);
        // then
        assertTrue(validate);
    }


    @Test
    void validate_institutionNotInAllowedList() throws JsonProcessingException {
        // given

        final String productId = "prod-io";
        final String institutionExternalId = "inst-2";
        final ConfigMapAllowedListOnboardingValidationStrategy validationStrategy =
                new ConfigMapAllowedListOnboardingValidationStrategy(Optional.of("{'prod-io':['inst-1']}"));
        // when
        final boolean validate = validationStrategy.validate(productId, institutionExternalId);
        // then
        assertFalse(validate);
    }

}