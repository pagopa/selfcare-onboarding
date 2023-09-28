package it.pagopa.selfcare.client.auth;

import io.quarkiverse.openapi.generator.OpenApiGeneratorConfig;
import io.quarkiverse.openapi.generator.providers.AbstractAuthenticationPropagationHeadersFactory;
import io.quarkiverse.openapi.generator.providers.HeadersProvider;
import jakarta.inject.Inject;

public class AuthenticationPropagationHeadersFactory extends AbstractAuthenticationPropagationHeadersFactory {

    @Inject
    public AuthenticationPropagationHeadersFactory(CompositeAuthenticationProvider compositeProvider, OpenApiGeneratorConfig generatorConfig, HeadersProvider headersProvider) {
        super(compositeProvider, generatorConfig, headersProvider);
    }

}