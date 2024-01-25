package it.pagopa.selfcare.onboarding.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import io.vertx.core.json.jackson.DatabindCodec;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.onboarding.crypto.*;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class OnboardingFunctionConfig {



    @Produces
    public ObjectMapper objectMapper(){
        ObjectMapper mapper =  DatabindCodec.mapper();
        //mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES)); // mandatory config

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);// custom config
        mapper.registerModule(new JavaTimeModule());                               // custom config
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);            // custom config
       // mapper.registerModule(new Jdk8Module());                                   // custom config
        return mapper;
    }

    @ApplicationScoped
    public AzureBlobClient azureBobClientContract(AzureStorageConfig azureStorageConfig){
        return new AzureBlobClientDefault(azureStorageConfig.connectionStringContract(), azureStorageConfig.containerContract());
    }

    @ApplicationScoped
    public ProductService productService(AzureStorageConfig azureStorageConfig){
       return new ProductServiceCacheable(azureStorageConfig.connectionStringProduct(), azureStorageConfig.containerProduct(), azureStorageConfig.productFilepath());
    }

    @Produces
    @IfBuildProperty(name = "onboarding-functions.pagopa-signature.source", stringValue = "aruba")
    public Pkcs7HashSignService arubaPkcs7HashSignService(){
        return new ArubaPkcs7HashSignServiceImpl(new ArubaSignServiceImpl());
    }


    @Produces
    @IfBuildProperty(name = "onboarding-functions.pagopa-signature.source", stringValue = "disabled")
    public Pkcs7HashSignService disabledPkcs7HashSignService(){
        return new Pkcs7HashSignService(){
            @Override
            public byte[] sign(InputStream inputStream) throws IOException {
                return new byte[0];
            }
        };
    }
    @Produces
    @DefaultBean
    public Pkcs7HashSignService pkcs7HashSignService(){
        return new Pkcs7HashSignServiceImpl();
    }
    @Produces
    public PadesSignService padesSignService(Pkcs7HashSignService pkcs7HashSignService){
        return new PadesSignServiceImpl(pkcs7HashSignService);
    }

}
