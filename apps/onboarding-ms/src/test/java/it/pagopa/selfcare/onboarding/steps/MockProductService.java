package it.pagopa.selfcare.onboarding.steps;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.List;
import java.util.Map;

@Alternative
@Priority(1)
@ApplicationScoped
public class MockProductService implements ProductService {

    @Override
    public List<Product> getProducts(boolean rootOnly, boolean valid) {
        Product product = new Product();
        product.setId("prod-io");
        return List.of(product);
    }

    @Override
    public void validateRoleMappings(Map<PartyRole, ? extends ProductRoleInfo> roleMappings) {

    }

    @Override
    public Product getProduct(String productId) {
        return null;
    }

    @Override
    public Product getProductRaw(String productId) {
        return null;
    }

    @Override
    public Product getProductIsValid(String productId) {
        Product product = new Product();
        product.setId("prod-io");
        return product;
    }

    @Override
    public ProductRole validateProductRole(String productId, String productRole, PartyRole role) {
        return null;
    }
}