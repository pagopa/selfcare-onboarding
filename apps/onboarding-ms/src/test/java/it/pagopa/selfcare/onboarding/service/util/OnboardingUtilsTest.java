package it.pagopa.selfcare.onboarding.service.util;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.entity.AdditionalInformations;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.wildfly.common.Assert;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class OnboardingUtilsTest {


    @ParameterizedTest
    @ValueSource(strings = {"ipa", "regulatedMarket", "establishedByRegulatoryProvision", "agentOfPublicService"})
    void shouldOnboardingInstitutionWithAdditionalInfo(String type) {
        Onboarding onboarding =  new Onboarding();
        Institution institution =  new Institution();
        institution.setInstitutionType(InstitutionType.GSP);
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
        onboarding.setAdditionalInformations(createSimpleAdditionalInformations(type));

        UniAssertSubscriber<Onboarding> subscriber = OnboardingUtils
                .customValidationOnboardingData(onboarding)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        Onboarding actual = subscriber.awaitItem().getItem();
        Assert.assertNotNull(actual);
    }

    @Test
    void shouldOnboardingInstitutionWithAdditionalInfoRequiredException() {

        Onboarding onboarding =  new Onboarding();
        Institution institution =  new Institution();
        institution.setInstitutionType(InstitutionType.GSP);
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
        onboarding.setAdditionalInformations(createSimpleAdditionalInformations("other"));

        UniAssertSubscriber<Onboarding> subscriber = OnboardingUtils
                .customValidationOnboardingData(onboarding)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);

    }

    @Test
    void shouldOnboardingInstitutionWithOtherNoteRequiredException() {

        Onboarding onboarding =  new Onboarding();
        Institution institution =  new Institution();
        institution.setInstitutionType(InstitutionType.GSP);
        onboarding.setInstitution(institution);
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());

        UniAssertSubscriber<Onboarding> subscriber = OnboardingUtils
                .customValidationOnboardingData(onboarding)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class);

    }


    private static AdditionalInformations createSimpleAdditionalInformations(String type) {
        AdditionalInformations additionalInformations = new AdditionalInformations();
        switch (type) {
            case "ipa":
                additionalInformations.setIpa(true);
                additionalInformations.setBelongRegulatedMarket(false);
                additionalInformations.setEstablishedByRegulatoryProvision(false);
                additionalInformations.setAgentOfPublicService(false);
                break;
            case "regulatedMarket":
                additionalInformations.setIpa(false);
                additionalInformations.setBelongRegulatedMarket(true);
                additionalInformations.setEstablishedByRegulatoryProvision(false);
                additionalInformations.setAgentOfPublicService(false);
                break;
            case "establishedByRegulatoryProvision":
                additionalInformations.setIpa(false);
                additionalInformations.setBelongRegulatedMarket(false);
                additionalInformations.setEstablishedByRegulatoryProvision(true);
                additionalInformations.setAgentOfPublicService(false);
                break;
            case "agentOfPublicService":
                additionalInformations.setIpa(false);
                additionalInformations.setBelongRegulatedMarket(false);
                additionalInformations.setEstablishedByRegulatoryProvision(false);
                additionalInformations.setAgentOfPublicService(true);
                break;
            default:
                additionalInformations.setIpa(false);
                additionalInformations.setBelongRegulatedMarket(false);
                additionalInformations.setEstablishedByRegulatoryProvision(false);
                additionalInformations.setAgentOfPublicService(false);
        }

        return additionalInformations;
    }
}
