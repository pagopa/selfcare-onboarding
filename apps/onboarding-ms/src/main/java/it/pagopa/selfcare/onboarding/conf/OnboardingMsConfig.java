package it.pagopa.selfcare.onboarding.conf;

import io.quarkus.runtime.StartupEvent;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Slf4j
@Data
public class OnboardingMsConfig {

    @ConfigProperty(name = "onboarding-ms.blob-storage.container-product")
    String containerProduct;

    @ConfigProperty(name = "onboarding-ms.blob-storage.filepath-product")
    String filepathProduct;

    @ConfigProperty(name = "onboarding-ms.blob-storage.connection-string-product")
    String connectionStringProduct;

    @ConfigProperty(name = "onboarding-ms.blob-storage.path-contracts")
    String contractPath;

    @ConfigProperty(name = "onboarding-ms.blob-storage.path-aggregates")
    String aggregatesPath;

    @ConfigProperty(name = "onboarding-ms.blob-storage.path-contracts-deleted")
    String deletedPath;

    void onStart(@Observes StartupEvent ev) {
        log.info(String.format("Database %s is starting...", Onboarding.mongoDatabase().getName()));
    }

    @ApplicationScoped
    public ProductService productService(){
        return new ProductServiceCacheable(connectionStringProduct, containerProduct, filepathProduct);
    }

    @ApplicationScoped
    public AzureBlobClient azureBobClientContract(@ConfigProperty(name = "onboarding-ms.blob-storage.connection-string-contracts")
                                                      String connectionStringContracts,
                                                  @ConfigProperty(name = "onboarding-ms.blob-storage.container-contracts")
                                                      String containerContracts){
        return new AzureBlobClientDefault(connectionStringContracts, containerContracts);
    }
}
