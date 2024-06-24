package it.pagopa.selfcare.onboarding.client.fd;

import it.pagopa.selfcare.onboarding.dto.OauthToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Form;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "fd-token")
@ApplicationScoped
@Path("/")
public interface FDTokenRestClient {
    @POST
    @Path("/auth/realms/fideiussioni-portal1/protocol/openid-connect/token")
    @Consumes("application/x-www-form-urlencoded; charset=UTF-8")
    OauthToken getFDToken(Form fdTokenParam);
}
