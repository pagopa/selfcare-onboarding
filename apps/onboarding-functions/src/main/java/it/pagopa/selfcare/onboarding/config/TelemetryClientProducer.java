package it.pagopa.selfcare.onboarding.config;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TelemetryClientProducer {

    @ConfigProperty(name = "onboarding-functions.appinsights.connection-string")
    String connectionString;

    @Produces
    @ApplicationScoped
    public TelemetryClient telemetryClient() {
        TelemetryConfiguration configuration = TelemetryConfiguration.createDefault();
        configuration.setConnectionString(connectionString);

        TelemetryClient client = new TelemetryClient(configuration);
        client.getContext().getOperation().setName("ONBOARDING-FUNCTIONS");

        return client;
    }
}
