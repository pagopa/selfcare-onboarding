package it.pagopa.selfcare.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.AzureBlobClientDefault;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ProductServiceCacheable implements ProductService{
    LocalDateTime productLastModifiedDate;
    private final AzureBlobClient azureBlobClient;
    private ProductServiceDefault productService;
    final String filePath;


    public ProductServiceCacheable(String connectionString, String containerName, String filePath ){
        this.azureBlobClient = new AzureBlobClientDefault(connectionString, containerName);
        this.filePath = filePath;
        refreshProduct();
    }

    public ProductServiceCacheable(AzureBlobClient azureBlobClient, String filePath) {
        this.azureBlobClient = azureBlobClient;
        this.filePath = filePath;
        refreshProduct();
    }

    public void refreshProduct(){
        LocalDateTime currentLastModifiedDate = azureBlobClient.getProperties(filePath).getLastModified().toLocalDateTime();
        if(productLastModifiedDate == null || currentLastModifiedDate.isAfter(productLastModifiedDate)){
            String productJsonString = azureBlobClient.getFileAsText(filePath);
            try{
                this.productService = new ProductServiceDefault(productJsonString);
            }catch(JsonProcessingException e){
                throw new IllegalArgumentException(e.getMessage());
            }
            this.productLastModifiedDate = currentLastModifiedDate;
        }
    }

    @Override
    public List<Product> getProducts(boolean rootOnly, boolean valid) {
        refreshProduct();
        return productService.getProducts(rootOnly, valid);
    }

    @Override
    public void validateRoleMappings(Map<PartyRole, ? extends ProductRoleInfo> roleMappings) {
        refreshProduct();
        productService.validateRoleMappings(roleMappings);
    }

    @Override
    public Product getProduct(String productId) {
        refreshProduct();
        return productService.getProduct(productId);
    }

    @Override
    public void fillContractTemplatePathAndVersion(Product product, InstitutionType institutionType) {
        refreshProduct();
        productService.fillContractTemplatePathAndVersion(product, institutionType);
    }

    @Override
    public Product getProductIsValid(String productId) {
        refreshProduct();
        return productService.getProductIsValid(productId);
    }
}
