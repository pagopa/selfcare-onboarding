package it.pagopa.selfcare.onboarding.service.util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;

@ApplicationScoped
public class OnboardingUtils {

    @RestClient
    @Inject
    UoApi uoApi;

    @RestClient
    @Inject
    AooApi aooApi;

    @RestClient
    @Inject
    InfocamerePdndApi infocamerePdndApi;

    public static final String ADDITIONAL_INFORMATION_REQUIRED = "Additional Information is required when institutionType is GSP and productId is pagopa";
    public static final String OTHER_NOTE_REQUIRED = "Other Note is required when other boolean are false";
    public static final String BILLING_OR_RECIPIENT_CODE_REQUIRED = "Billing and/or recipient code are required";
    public static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE = "Institution with external id '%s' is not allowed to onboard '%s' product because it is not delegable";
    public static final String PARENT_TAX_CODE_IS_INVALID = "The tax code of the parent entity of the request does not match the tax code of the parent entity retrieved by IPA";
    public static final String TAX_CODE_INVOICING_IS_INVALID = "The tax code invoicing of the request does not match any tax code of institutions' hierarchy";

}
