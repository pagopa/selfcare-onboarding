package it.pagopa.selfcare.product.service;


import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;

import java.util.List;
import java.util.Map;

public interface ProductService {
    public List<Product> getProducts(boolean rootOnly, boolean valid) ;

    void validateRoleMappings(Map<PartyRole, ? extends ProductRoleInfo> roleMappings);

    Product getProduct(String productId);

    void fillContractTemplatePathAndVersion(Product product, InstitutionType institutionType);

    Product getProductIsValid(String productId);

    ProductRole validateProductRole(String productId, String productRole, PartyRole role);
}
