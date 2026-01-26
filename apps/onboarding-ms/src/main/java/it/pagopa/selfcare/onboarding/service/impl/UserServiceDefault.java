package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

import java.util.List;

@ApplicationScoped
@Slf4j
public class UserServiceDefault implements UserService {

    @Inject
    @RestClient
    InstitutionApi userInstitutionApi;

    @Override
    public Uni<List<UserInstitutionResponse>> retrieveUserInstitutions(String institutionId, List<String> productRoles, List<String> products, List<String> roles, List<String> states, String userId) {
        return userInstitutionApi.retrieveUserInstitutions(institutionId, productRoles, products, roles, states, userId);
    }

}
