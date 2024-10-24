package it.pagopa.selfcare.onboarding;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.AggregateInstitution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapperImpl;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;

@QuarkusTest
class OnboardingMapperTest {

    @Inject
    private OnboardingMapperImpl onboardingMapper;

    @Test
    void mapToOnboardingAggregateOrchestratorInputTestWithAggregateUsers(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("example");
        onboarding.setProductId("productId");
        User user = new User();
        user.setId("aggregatorUser");
        user.setRole(PartyRole.MANAGER);
        user.setProductRole("aggregatorProductRole");
        onboarding.setUsers(List.of(user));

        User user2 = new User();
        user2.setId("aggregateUser");
        user2.setRole(PartyRole.MANAGER);
        user2.setProductRole("aggregateProductRole");
        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(List.of(user2));
        onboarding.setAggregates(List.of(aggregateInstitution));

        OnboardingAggregateOrchestratorInput response = onboardingMapper.mapToOnboardingAggregateOrchestratorInput(onboarding, aggregateInstitution);
        Assertions.assertEquals(1, response.getUsers().size());
        Assertions.assertEquals("aggregateUser", response.getUsers().get(0).getId());
        Assertions.assertEquals(PartyRole.MANAGER, response.getUsers().get(0).getRole());
        Assertions.assertEquals("aggregateProductRole", response.getUsers().get(0).getProductRole());

    }

    @Test
    void mapToOnboardingAggregateOrchestratorInputTestWithoutAggregatesUser(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("example");
        onboarding.setProductId("productId");
        User user = new User();
        user.setId("aggregatorUser");
        user.setRole(PartyRole.MANAGER);
        user.setProductRole("aggregatorProductRole");
        onboarding.setUsers(List.of(user));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(null);
        onboarding.setAggregates(List.of(aggregateInstitution));

        OnboardingAggregateOrchestratorInput response = onboardingMapper.mapToOnboardingAggregateOrchestratorInput(onboarding, aggregateInstitution);
        Assertions.assertEquals(1, response.getUsers().size());
        Assertions.assertEquals("aggregatorUser", response.getUsers().get(0).getId());
        Assertions.assertEquals(PartyRole.MANAGER, response.getUsers().get(0).getRole());
        Assertions.assertEquals("aggregatorProductRole", response.getUsers().get(0).getProductRole());
    }

    @Test
    void mapToOnboardingAggregateOrchestratorInputTestWithoutAggregatesUserProdPagoPA(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("example");
        onboarding.setProductId(PROD_PAGOPA.getValue());
        User user = new User();
        user.setId("aggregatorUser");
        user.setRole(PartyRole.MANAGER);
        user.setProductRole("aggregatorProductRole");
        onboarding.setUsers(List.of(user));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(null);
        onboarding.setAggregates(List.of(aggregateInstitution));

        OnboardingAggregateOrchestratorInput response = onboardingMapper.mapToOnboardingAggregateOrchestratorInput(onboarding, aggregateInstitution);
        Assertions.assertEquals(1, response.getUsers().size());
        Assertions.assertEquals("aggregatorUser", response.getUsers().get(0).getId());
        Assertions.assertEquals(PartyRole.ADMIN_EA, response.getUsers().get(0).getRole());
        Assertions.assertEquals("aggregatorProductRole", response.getUsers().get(0).getProductRole());
    }

    @Test
    void mapToOnboardingAggregateOrchestratorInputTestWithAggregatesUserEmptyList(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("example");
        onboarding.setProductId("productId");
        User user = new User();
        user.setId("aggregatorUser");
        user.setRole(PartyRole.MANAGER);
        user.setProductRole("aggregatorProductRole");
        onboarding.setUsers(List.of(user));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(Collections.emptyList());
        onboarding.setAggregates(List.of(aggregateInstitution));

        OnboardingAggregateOrchestratorInput response = onboardingMapper.mapToOnboardingAggregateOrchestratorInput(onboarding, aggregateInstitution);
        Assertions.assertEquals(1, response.getUsers().size());
        Assertions.assertEquals("aggregatorUser", response.getUsers().get(0).getId());
        Assertions.assertEquals(PartyRole.MANAGER, response.getUsers().get(0).getRole());
        Assertions.assertEquals("aggregatorProductRole", response.getUsers().get(0).getProductRole());
    }
}
