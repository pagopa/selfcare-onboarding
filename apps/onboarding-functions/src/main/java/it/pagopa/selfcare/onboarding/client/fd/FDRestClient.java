package it.pagopa.selfcare.onboarding.client.fd;

import it.pagopa.selfcare.onboarding.dto.OrganizationLightBeanResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "fd")
@ApplicationScoped
@Path("/")
public interface FDRestClient {
    @GET
    @Path("/api/organizationPA/checkOrganization")
    OrganizationLightBeanResponse checkOrganization(@QueryParam("codiceFiscale") String fiscalCode, @QueryParam("partitaIva") String vatNumber);
}
