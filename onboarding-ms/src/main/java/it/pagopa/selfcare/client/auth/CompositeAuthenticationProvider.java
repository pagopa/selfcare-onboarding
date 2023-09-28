package it.pagopa.selfcare.client.auth;

import io.quarkiverse.openapi.generator.OpenApiGeneratorConfig;
import io.quarkiverse.openapi.generator.providers.AbstractCompositeAuthenticationProvider;
import io.quarkiverse.openapi.generator.providers.BearerAuthenticationProvider;
import io.quarkiverse.openapi.generator.providers.OperationAuthInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;

@Priority(Priorities.AUTHENTICATION)
public class CompositeAuthenticationProvider extends AbstractCompositeAuthenticationProvider {

    @Inject
    OpenApiGeneratorConfig generatorConfig;


    @PostConstruct
    public void init() {

        BearerAuthenticationProvider bearerProvider0 = new BearerAuthenticationProvider("product_json", sanitizeAuthName("bearerAuth"), "bearer", generatorConfig);
        this.addAuthenticationProvider(bearerProvider0);
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/")
            .withId("createProductUsingPOST")
            .withMethod("POST")
            .build());
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/{id}/sub-products")
            .withId("createSubProductUsingPOST")
            .withMethod("POST")
            .build());
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/{id}")
            .withId("deleteProductUsingDELETE")
            .withMethod("DELETE")
            .build());
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/{id}/valid")
            .withId("getProductIsValidUsingGET")
            .withMethod("GET")
            .build());
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/{id}/role-mappings")
            .withId("getProductRolesUsingGET")
            .withMethod("GET")
            .build());
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/{id}")
            .withId("getProductUsingGET")
            .withMethod("GET")
            .build());
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/tree")
            .withId("getProductsTreeUsingGET")
            .withMethod("GET")
            .build());
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/")
            .withId("getProductsUsingGET")
            .withMethod("GET")
            .build());
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/{id}/depict-image")
            .withId("saveProductDepictImageUsingPUT")
            .withMethod("PUT")
            .build());
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/{id}/logo")
            .withId("saveProductLogoUsingPUT")
            .withMethod("PUT")
            .build());
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/{id}/status/{status}")
            .withId("updateProductStatusUsingPUT")
            .withMethod("PUT")
            .build());
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/{id}")
            .withId("updateProductUsingPUT")
            .withMethod("PUT")
            .build());
        bearerProvider0.addOperation(OperationAuthInfo.builder()
            .withPath("/products/{id}/sub-products")
            .withId("updateSubProductUsingPUT")
            .withMethod("PUT")
            .build());
    }

}