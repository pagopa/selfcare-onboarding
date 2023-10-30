package it.pagopa.selfcare.product.service;


import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;

import java.util.List;
import java.util.Map;

public interface ProductService {
    List<Product> getProducts(boolean rootOnly);

    void validateRoleMappings(Map<PartyRole, ? extends ProductRoleInfo> roleMappings);

    Product getProduct(String id, InstitutionType institutionType);

    Product getProductIsValid(String id);
}
