package it.pagopa.selfcare.onboarding.steps;

import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.client.auth.AuthenticationPropagationHeadersFactory;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import org.eclipse.microprofile.config.ConfigProvider;

@Alternative
@Priority(1)
@ApplicationScoped
@TestProfile(IntegrationProfile.class)
public class IntegrationAuthenticationHeadersFactory
    extends AuthenticationPropagationHeadersFactory {

  @Override
  public MultivaluedMap<String, String> update(
      MultivaluedMap<String, String> incomingHeaders,
      MultivaluedMap<String, String> clientOutgoingHeaders) {
    clientOutgoingHeaders.put("Authorization", List.of(ConfigProvider.getConfig().getValue("BEARER_TOKEN", String.class)));
    return clientOutgoingHeaders;
  }
}
