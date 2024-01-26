package it.pagopa.selfcare.onboarding.conf;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OnboardingMsConfig {

    @ConfigProperty(name = "onboarding-ms.blob-storage.container-product")
    String containerProduct;

    @ConfigProperty(name = "onboarding-ms.blob-storage.filepath-product")
    String filepathProduct;

    @ConfigProperty(name = "onboarding-ms.blob-storage.connection-string-product")
    String connectionStringProduct;

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
