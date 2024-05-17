package it.pagopa.selfcare.onboarding.service.util;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.entity.AdditionalInformations;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;
import org.wildfly.common.Assert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class OnboardingUtilsTest {

    @InjectMock
    @RestClient
    UoApi uoApi;
    @Inject
    OnboardingUtils onboardingUtils;

    @ParameterizedTest
    @ValueSource(strings = {"ipa", "regulatedMarket", "establishedByRegulatoryProvision", "agentOfPublicService"})
    void shouldOnboardingInstitutionWithAdditionalInfo(String type) {

        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        Onboarding onboarding =  new Onboarding();
        Institution institution =  new Institution();
        institution.setInstitutionType(InstitutionType.GSP);
        onboarding.setBilling(billing);
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
        onboarding.setAdditionalInformations(createSimpleAdditionalInformations(type));

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        Onboarding actual = subscriber.awaitItem().getItem();
        Assert.assertNotNull(actual);
    }

    @Test
    void shouldOnboardingInstitutionWithAdditionalInfoRequiredException() {

        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        Onboarding onboarding =  new Onboarding();
        Institution institution =  new Institution();
        institution.setInstitutionType(InstitutionType.GSP);
        onboarding.setBilling(billing);
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
        onboarding.setAdditionalInformations(createSimpleAdditionalInformations("other"));

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);

    }

    @Test
    void shouldOnboardingInstitutionWithOtherNoteRequiredException() {

        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        Onboarding onboarding =  new Onboarding();
        Institution institution =  new Institution();
        institution.setInstitutionType(InstitutionType.GSP);
        onboarding.setBilling(billing);
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);

    }

    @Test
    void shouldOnboardingInstitutionWithBillingRequiredException() {

        Onboarding onboarding =  new Onboarding();
        Institution institution =  new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);

    }

    @Test
    void shouldOnboardingInstitutionWithRecipientCodeRequiredException() {

        Billing billing = new Billing();
        Onboarding onboarding =  new Onboarding();
        Institution institution =  new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitution(institution);
        onboarding.setBilling(billing);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);

    }

    @Test
    void shouldOnboardingInstitutionWithParentTaxCodeException() {

        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        institution.setTaxCode("taxCode1");
        UOResource uoResource = new UOResource();
        uoResource.setCodiceFiscaleEnte("taxCode2");
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);

        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .validationTaxCodeInvoicing(onboarding, dummyProduct())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void shouldOnboardingInstitutionWithTaxCodeInvoicingException() {

        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        institution.setTaxCode("taxCode");
        UOResource uoResource = new UOResource();
        uoResource.setCodiceFiscaleEnte("taxCode");
        uoResource.setCodiceFiscaleSfe("taxCodeInvoicing1");
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);

        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .validationTaxCodeInvoicing(onboarding, dummyProduct())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }


    private static AdditionalInformations createSimpleAdditionalInformations(String type) {
        AdditionalInformations additionalInformations = new AdditionalInformations();
        switch (type) {
            case "ipa" -> {
                additionalInformations.setIpa(true);
                additionalInformations.setBelongRegulatedMarket(false);
                additionalInformations.setEstablishedByRegulatoryProvision(false);
                additionalInformations.setAgentOfPublicService(false);
            }
            case "regulatedMarket" -> {
                additionalInformations.setIpa(false);
                additionalInformations.setBelongRegulatedMarket(true);
                additionalInformations.setEstablishedByRegulatoryProvision(false);
                additionalInformations.setAgentOfPublicService(false);
            }
            case "establishedByRegulatoryProvision" -> {
                additionalInformations.setIpa(false);
                additionalInformations.setBelongRegulatedMarket(false);
                additionalInformations.setEstablishedByRegulatoryProvision(true);
                additionalInformations.setAgentOfPublicService(false);
            }
            case "agentOfPublicService" -> {
                additionalInformations.setIpa(false);
                additionalInformations.setBelongRegulatedMarket(false);
                additionalInformations.setEstablishedByRegulatoryProvision(false);
                additionalInformations.setAgentOfPublicService(true);
            }
            default -> {
                additionalInformations.setIpa(false);
                additionalInformations.setBelongRegulatedMarket(false);
                additionalInformations.setEstablishedByRegulatoryProvision(false);
                additionalInformations.setAgentOfPublicService(false);
            }
        }

        return additionalInformations;
    }

    private Product dummyProduct() {
        return new Product();
    }
}
