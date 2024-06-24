package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.openapi.quarkus.core_json.model.DataProtectionOfficerResponse;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.core_json.model.PaymentServiceProviderResponse;
import org.openapi.quarkus.core_json.model.RootParentResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.model.GeographicTaxonomyResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class NotificationBuilderTestUtil {

    public static final String institutionId = "i1";
    public static final String tokenId = "t1";
    public static final String productId = "prod";

    public static Token createToken() {
        Token token = new Token();
        token.setId(tokenId);
        token.setProductId(productId);
        token.setContractSigned("contractSigned");
        return token;
    }

    public static InstitutionResponse createInstitution() {
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setAddress("address");
        institutionResponse.setAooParentCode("aooParentCode");
        institutionResponse.setAttributes(new ArrayList<>());
        institutionResponse.setBusinessRegisterPlace("businessRegisterPlace");
        institutionResponse.setCity("city");
        institutionResponse.setCountry("country");
        institutionResponse.setCounty("county");
        institutionResponse.setCreatedAt(OffsetDateTime.now());
        institutionResponse.setDataProtectionOfficer(new DataProtectionOfficerResponse());
        institutionResponse.setDelegation(true);
        institutionResponse.setDescription("description");
        institutionResponse.setDigitalAddress("digitalAddress");
        institutionResponse.setExternalId("externalId");
        institutionResponse.setGeographicTaxonomies(new ArrayList<>());
        institutionResponse.setId(institutionId);
        institutionResponse.setImported(true);
        institutionResponse.setInstitutionType(InstitutionResponse.InstitutionTypeEnum.PA);
        institutionResponse.setIstatCode("istatCode");
        institutionResponse.setOnboarding(new ArrayList<>());
        institutionResponse.setOrigin("IPA");
        institutionResponse.setOriginId("originId");
        institutionResponse.setPaymentServiceProvider(mockPaymentServiceProvider());
        institutionResponse.setRea("rea");
        institutionResponse.setRootParent(new RootParentResponse());
        institutionResponse.setShareCapital("shareCapital");
        institutionResponse.setSubunitCode("subunitCode");
        institutionResponse.setSubunitType("subunitType");
        institutionResponse.setSupportEmail("supportEmail");
        institutionResponse.setSupportPhone("supportPhone");
        institutionResponse.setTaxCode("taxCode");
        institutionResponse.setUpdatedAt(OffsetDateTime.now());
        institutionResponse.setZipCode("zipCode");

        return institutionResponse;
    }

    public static PaymentServiceProviderResponse mockPaymentServiceProvider() {
        PaymentServiceProviderResponse paymentServiceProvider = new PaymentServiceProviderResponse();
        paymentServiceProvider.setAbiCode("abiCode");
        paymentServiceProvider.setBusinessRegisterNumber("businessRegisterNumber");
        paymentServiceProvider.setLegalRegisterName("legalRegisterName");
        paymentServiceProvider.setLegalRegisterNumber("legalRegisterNumber");
        paymentServiceProvider.setVatNumberGroup(true);
        return paymentServiceProvider;
    }

    public static Onboarding createOnboarding(OnboardingStatus status, OffsetDateTime createdAt, OffsetDateTime activatedAt, OffsetDateTime updateAt, OffsetDateTime deletedAt) {
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(productId);
        onboarding.setId(tokenId);
        onboarding.setStatus(status);
        onboarding.setCreatedAt(Objects.nonNull(createdAt) ? createdAt.toLocalDateTime() : null);
        onboarding.setActivatedAt(Objects.nonNull(activatedAt) ? activatedAt.toLocalDateTime() : null);
        onboarding.setUpdatedAt(Objects.nonNull(updateAt) ? updateAt.toLocalDateTime() : null);
        onboarding.setDeletedAt(Objects.nonNull(deletedAt) ? deletedAt.toLocalDateTime() : null);

        Institution institution = new Institution();
        institution.setId(institutionId);
        onboarding.setInstitution(institution);
        return onboarding;
    }

    public static void mockPartyRegistryProxy(InstitutionApi registryProxyInstitutionsApi, GeographicTaxonomiesApi geographicTaxonomiesApi, InstitutionResponse institution) {
        InstitutionResource institutionProxyInfoMock = mockInstitutionResource();
        institutionProxyInfoMock.setTaxCode(institution.getExternalId());

        GeographicTaxonomyResource geographicTaxonomiesMock = mockGeographicTaxonomies();
        geographicTaxonomiesMock.setIstatCode(institutionProxyInfoMock.getIstatCode());

        when(registryProxyInstitutionsApi.findInstitutionUsingGET(any(), any(), any()))
                .thenReturn(institutionProxyInfoMock);
        when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any())).thenReturn(geographicTaxonomiesMock);
    }

    public static GeographicTaxonomyResource mockGeographicTaxonomies() {
        GeographicTaxonomyResource geographicTaxonomiesMock = new GeographicTaxonomyResource();
        geographicTaxonomiesMock.setIstatCode("istatCode");
        geographicTaxonomiesMock.setCode("code");
        geographicTaxonomiesMock.setProvinceId("province");
        geographicTaxonomiesMock.setProvinceAbbreviation("provinceAbbreviation");
        geographicTaxonomiesMock.setRegionId("region");
        geographicTaxonomiesMock.setCountry("country");
        geographicTaxonomiesMock.setCountryAbbreviation("countryAbbreviation");
        geographicTaxonomiesMock.setDesc("desc");
        return geographicTaxonomiesMock;
    }

    public static InstitutionResource mockInstitutionResource() {
        InstitutionResource institutionProxyInfoMock = new InstitutionResource();
        institutionProxyInfoMock.setIstatCode("istatCode");
        institutionProxyInfoMock.setAddress("address");
        institutionProxyInfoMock.setAoo("aoo");
        institutionProxyInfoMock.setCategory("category");
        institutionProxyInfoMock.setId("id");
        institutionProxyInfoMock.setDescription("description");
        return institutionProxyInfoMock;
    }
}
