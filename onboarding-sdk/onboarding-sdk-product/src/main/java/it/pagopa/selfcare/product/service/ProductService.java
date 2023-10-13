package it.pagopa.selfcare.product.service;


import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.commons.base.utils.InstitutionType;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;

import java.util.EnumMap;
import java.util.List;

public interface ProductService {
    List<Product> getProducts(boolean rootOnly);

    void validateRoleMappings(EnumMap<PartyRole, ? extends ProductRoleInfo> roleMappings);

    Product getProduct(String id, InstitutionType institutionType);

    Product getProductIsValid(String id);
}
