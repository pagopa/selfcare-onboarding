package it.pagopa.selfcare.onboarding.service.util;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.entity.AdditionalInformations;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.inject.Inject;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOsResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOsResource;
import org.wildfly.common.Assert;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class OnboardingUtilsTest {

    @InjectMock
    @RestClient
    UoApi uoApi;

    @InjectMock
    @RestClient
    AooApi aooApi;

    @Inject
    OnboardingUtils onboardingUtils;

    @ParameterizedTest
    @ValueSource(strings = {"ipa", "regulatedMarket", "establishedByRegulatoryProvision", "agentOfPublicService"})
    void shouldOnboardingInstitutionWithAdditionalInfo(String type) {

        Billing billing = new Billing();
        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.GSP);
        onboarding.setBilling(billing);
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
        onboarding.setAdditionalInformations(createSimpleAdditionalInformations(type));

        OnboardingUtils.ProxyResource proxyResource = OnboardingUtils.ProxyResource.builder()
                .type(InstitutionPaSubunitType.EC)
                .build();

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct(), proxyResource)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        Onboarding actual = subscriber.awaitItem().getItem();
        Assert.assertNotNull(actual);
    }

    @Test
    void shouldOnboardingInstitutionWithAdditionalInfoRequiredException() {

        Billing billing = new Billing();
        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.GSP);
        onboarding.setBilling(billing);
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
        onboarding.setAdditionalInformations(createSimpleAdditionalInformations("other"));

        OnboardingUtils.ProxyResource proxyResource = OnboardingUtils.ProxyResource.builder()
                .type(InstitutionPaSubunitType.EC)
                .build();

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct(), proxyResource)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);

    }

    @Test
    void shouldOnboardingInstitutionWithOtherNoteRequiredException() {

        Billing billing = new Billing();
        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.GSP);
        onboarding.setBilling(billing);
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());

        OnboardingUtils.ProxyResource proxyResource = OnboardingUtils.ProxyResource.builder()
                .type(InstitutionPaSubunitType.EC)
                .build();

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct(), proxyResource)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);

    }

    @Test
    void shouldOnboardingInstitutionWithBillingRequiredException() {

        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PN.getValue());
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);

        OnboardingUtils.ProxyResource proxyResource = OnboardingUtils.ProxyResource.builder()
                .type(InstitutionPaSubunitType.EC)
                .build();

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct(), proxyResource)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);

    }

    @Test
    void shouldOnboardingInstitutionWithRecipientCodeRequiredException() {

        Billing billing = new Billing();
        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitution(institution);
        onboarding.setBilling(billing);
        onboarding.setProductId(ProductId.PROD_PN.getValue());

        OnboardingUtils.ProxyResource proxyResource = OnboardingUtils.ProxyResource.builder()
                .type(InstitutionPaSubunitType.EC)
                .build();

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct(), proxyResource)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);

    }

    @Test
    void shouldOnboardingInstitutionWithParentTaxCodeException() {

        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setSubunitCode("subunitCode");
        institution.setSubunitType(InstitutionPaSubunitType.UO);
        institution.setInstitutionType(InstitutionType.PA);
        institution.setTaxCode("taxCode1");
        UOResource uoResource = new UOResource();
        uoResource.setCodiceFiscaleEnte("taxCode2");
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);

        OnboardingUtils.ProxyResource proxyResource = OnboardingUtils.ProxyResource.builder()
                .type(InstitutionPaSubunitType.UO)
                .resource(uoResource)
                .build();

        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct(), proxyResource)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void shouldOnboardingInstitutionWithTaxCodeInvoicingException() {

        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setSubunitCode("subunitCode");
        institution.setSubunitType(InstitutionPaSubunitType.UO);
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

        UOResource uoResource2 = new UOResource();
        uoResource2.setCodiceFiscaleEnte("taxCode1");
        uoResource2.setCodiceFiscaleSfe("taxCodeInvoicing1");
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());

        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        UOsResource uOsResource = new UOsResource();
        uOsResource.setItems(List.of(uoResource, uoResource2));
        when(uoApi.findAllUsingGET1(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(uOsResource));

        OnboardingUtils.ProxyResource proxyResource = OnboardingUtils.ProxyResource.builder()
                .type(InstitutionPaSubunitType.UO)
                .resource(uoResource)
                .build();


        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct(), proxyResource)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void checkRecipientCodeNoBilling() {

        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setOriginId("ipaCode");
        institution.setSubunitCode("subunitCode");
        institution.setSubunitType(InstitutionPaSubunitType.AOO);
        institution.setInstitutionType(InstitutionType.PA);
        institution.setTaxCode("taxCode");
        AOOResource resource = new AOOResource();
        resource.setCodiceFiscaleEnte("taxCode");
        resource.setCodiceIpa("ipaCode");
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_IO_SIGN.getValue());
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        billing.setRecipientCode("recipientCode");
        onboarding.setBilling(billing);

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("ipaCode");
        uoResource.setCodiceFiscaleEnte("taxCode1");

        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        when(aooApi.findByUnicodeUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(resource));

        AOOsResource uOsResource = new AOOsResource();
        uOsResource.setItems(List.of(resource));

        OnboardingUtils.ProxyResource proxyResource = OnboardingUtils.ProxyResource.builder()
                .type(InstitutionPaSubunitType.AOO)
                .resource(resource)
                .build();

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct(), proxyResource)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void checkRecipientCodeFailure() {

        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setOriginId("ipaCode");
        institution.setSubunitCode("subunitCode");
        institution.setSubunitType(InstitutionPaSubunitType.AOO);
        institution.setInstitutionType(InstitutionType.PA);
        institution.setTaxCode("taxCode");
        AOOResource resource = new AOOResource();
        resource.setCodiceFiscaleEnte("taxCode");
        resource.setCodiceIpa("ipaCode");
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_IO_SIGN.getValue());
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        billing.setRecipientCode("recipientCode");
        onboarding.setBilling(billing);

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("ipaCode");
        uoResource.setCodiceFiscaleEnte("taxCode1");

        ClientWebApplicationException exception = new ClientWebApplicationException(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().failure(exception));

        OnboardingUtils.ProxyResource proxyResource = OnboardingUtils.ProxyResource.builder()
                .type(InstitutionPaSubunitType.AOO)
                .resource(resource)
                .build();

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct(), proxyResource)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(Exception.class);
    }

    @Test
    void checkRecipientCodeUONoBilling() {

        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setOriginId("ipaCode");
        institution.setSubunitCode("subunitCode");
        institution.setSubunitType(InstitutionPaSubunitType.UO);
        institution.setInstitutionType(InstitutionType.PA);
        institution.setTaxCode("taxCode");
        AOOResource resource = new AOOResource();
        resource.setCodiceFiscaleEnte("taxCode");
        resource.setCodiceIpa("ipaCode");
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_IO_SIGN.getValue());
        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");
        onboarding.setBilling(billing);

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("ipaCode");
        uoResource.setCodiceFiscaleEnte("taxCode1");

        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        OnboardingUtils.ProxyResource proxyResource = OnboardingUtils.ProxyResource.builder()
                .type(InstitutionPaSubunitType.UO)
                .resource(uoResource)
                .build();

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct(), proxyResource)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void checkRecipientCodeNoAssociation() {

        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setOriginId("codiceIpe");
        institution.setSubunitCode("subunitCode");
        institution.setSubunitType(InstitutionPaSubunitType.AOO);
        institution.setInstitutionType(InstitutionType.PA);
        institution.setTaxCode("taxCode");
        AOOResource aooResource = new AOOResource();
        aooResource.setCodiceFiscaleEnte("taxCode");
        aooResource.setCodiceIpa("ipaCode");
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_IO_SIGN.getValue());
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        billing.setRecipientCode("recipientCode");
        onboarding.setBilling(billing);

        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("customIpaCode");
        uoResource.setCodiceFiscaleEnte("taxCode1");
        uoResource.setCodiceFiscaleSfe("taxCodeInvoicing1");

        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        when(aooApi.findByUnicodeUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(aooResource));

        AOOsResource uOsResource = new AOOsResource();
        uOsResource.setItems(List.of(aooResource));

        OnboardingUtils.ProxyResource proxyResource = OnboardingUtils.ProxyResource.builder()
                .type(InstitutionPaSubunitType.AOO)
                .resource(aooResource)
                .build();

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct(), proxyResource)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void checkRecipientCodeSuccess() {

        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setOriginId("ipaCode");
        institution.setSubunitCode("subunitCode");
        institution.setInstitutionType(InstitutionType.PA);
        institution.setTaxCode("taxCode");
        UOResource uoResource = new UOResource();
        uoResource.setCodiceFiscaleEnte("taxCode");
        uoResource.setCodiceIpa("ipaCode");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_IO_SIGN.getValue());
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        billing.setRecipientCode("recipientCode");
        onboarding.setBilling(billing);

        when(uoApi.findByUnicodeUsingGET1(any(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        UOsResource uOsResource = new UOsResource();
        uOsResource.setItems(List.of(uoResource));

        OnboardingUtils.ProxyResource proxyResource = OnboardingUtils.ProxyResource.builder()
                .type(InstitutionPaSubunitType.EC)
                .build();

        UniAssertSubscriber<Onboarding> subscriber = onboardingUtils
                .customValidationOnboardingData(onboarding, dummyProduct(), proxyResource)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        Onboarding actual = subscriber.awaitItem().getItem();
        Assert.assertNotNull(actual);
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
