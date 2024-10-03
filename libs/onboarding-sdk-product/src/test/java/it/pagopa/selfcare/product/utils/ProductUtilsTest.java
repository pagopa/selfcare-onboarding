package it.pagopa.selfcare.product.utils;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.PHASE_ADDITION_ALLOWED;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProductUtilsTest {

    private final static String productRoleManager = "admin";

    @Test
    void validRoles() {
        Product product = dummyProduct();
        List<PartyRole> partyRoles = ProductUtils.validRoles(product, PHASE_ADDITION_ALLOWED.ONBOARDING);
        assertEquals(1, partyRoles.size());
        assertEquals(PartyRole.MANAGER, partyRoles.get(0));
    }

    @Test
    void validRolesByProductRole() {
        Product product = dummyProduct();
        List<PartyRole> partyRoles = ProductUtils.validRolesByProductRole(product, PHASE_ADDITION_ALLOWED.ONBOARDING, productRoleManager);
        assertEquals(1, partyRoles.size());
        assertEquals(PartyRole.MANAGER, partyRoles.get(0));
    }

    Product dummyProduct() {

        ProductRole productRole = new ProductRole();
        productRole.setCode(productRoleManager);
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setRoles(List.of(productRole));
        productRoleInfo.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.ONBOARDING.value));

        ProductRole productRoleOperator = new ProductRole();
        productRoleOperator.setCode("operator");
        ProductRoleInfo productRoleInfoOperator = new ProductRoleInfo();
        productRoleInfoOperator.setRoles(List.of(productRoleOperator));
        productRoleInfoOperator.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.DASHBOARD.value));

        Map<PartyRole, ProductRoleInfo> roleMapping = new HashMap<>();
        roleMapping.put(PartyRole.MANAGER, productRoleInfo);
        roleMapping.put(PartyRole.OPERATOR, productRoleInfoOperator);

        Product productResource = new Product();
        productResource.setId("productId");
        productResource.setRoleMappings(roleMapping);
        return productResource;
    }
}
