package it.pagopa.selfcare.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.exception.InvalidRoleMappingException;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceDefaultTest {

    private static final String PRODUCT_JSON_STRING_EMPTY = "[]";
    private static final String PRODUCT_JSON_STRING = "[{\"id\":\"prod-test-parent\",\"status\":\"ACTIVE\"}," +
            "{\"id\":\"prod-test\", \"parentId\":\"prod-test-parent\",\"status\":\"ACTIVE\"}," +
            "{\"id\":\"prod-inactive\",\"status\":\"INACTIVE\"}]";

    final private String PRODUCT_JSON_STRING_WITH_ROLEMAPPING = "[{\"id\":\"prod-test-parent\",\"status\":\"ACTIVE\"}," +
            "{\"id\":\"prod-test\", \"parentId\":\"prod-test-parent\",\"status\":\"ACTIVE\", \"roleMappings\" : {\"MANAGER\":{\"roles\":[{\"code\":\"operatore\"}], \"phasesAdditionAllowed\":[\"onboarding\"]}}}," +
            "{\"id\":\"prod-inactive\",\"status\":\"INACTIVE\"}]";

    @Test
    void productServiceDefault_shouldThrowProductNotFoundExceptionIfJsonIsEmpty() {
        assertThrows(ProductNotFoundException.class, () -> new ProductServiceDefault(PRODUCT_JSON_STRING_EMPTY));
    }
    @Test
    void productServiceDefault_shouldNotThrowProductNotFoundExceptionIfJsonIsValid() {
        assertDoesNotThrow(() -> new ProductServiceDefault(PRODUCT_JSON_STRING));
    }
    @Test
    void productServiceDefault_shouldThrowProductNotFoundExceptionIfJsonIsEmptyAndMapper() {
        assertThrows(ProductNotFoundException.class, () -> new ProductServiceDefault(PRODUCT_JSON_STRING_EMPTY, new ObjectMapper()));
    }

    @Test
    void getProducts_shouldReturnProductsRootOnly() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        assertEquals( 2, productService.getProducts(true, false).size());
    }

    @Test
    void getProducts_shouldReturnProductsAll() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        assertEquals(3, productService.getProducts(false, false).size());
    }

    @Test
    void getProducts_shouldReturnProductsRootOnlyAndValid() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        assertEquals( 1, productService.getProducts(true, true).size());
    }

    @Test
    void getProducts_shouldReturnProductsAllAndValid() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        assertEquals(2, productService.getProducts(false, true).size());
    }

    @Test
    void validateRoleMappings_shouldThrowIllegalExceptionIfRoleMappingIsNull() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        assertThrows(IllegalArgumentException.class, () -> productService.validateRoleMappings(new HashMap<>()));
    }

    @Test
    void validateRoleMappings_shouldThrowIllegalExceptionIfProductRoleInfoIsNull() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        Map<PartyRole, ProductRoleInfo> roleMappings = new HashMap<>();
        roleMappings.put(PartyRole.MANAGER, null);
        assertThrows(IllegalArgumentException.class, () -> productService.validateRoleMappings(roleMappings));
    }

    @Test
    void validateRoleMappings_shouldThrowIllegalExceptionIfProductRoleInfoIsRolesEmpty() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        Map<PartyRole, ProductRoleInfo> roleMappings = new HashMap<>();
        roleMappings.put(PartyRole.MANAGER, new ProductRoleInfo());
        assertThrows(IllegalArgumentException.class, () -> productService.validateRoleMappings(roleMappings));
    }

    @Test
    void validateRoleMappings_shouldThrowInvalidRoleMappingExceptionIfProductRoleInfoIsRolesEmpty() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        Map<PartyRole, ProductRoleInfo> roleMappings = new HashMap<>();
        ProductRoleInfo productRoleInfo1 = new ProductRoleInfo();
        productRoleInfo1.setRoles(List.of(new ProductRole(), new ProductRole()));
        roleMappings.put(PartyRole.MANAGER, productRoleInfo1);
        assertThrows(InvalidRoleMappingException.class, () -> productService.validateRoleMappings(roleMappings));
    }

    @Test
    void getProduct_shouldThrowIllegalExceptionIfIdNull() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        assertThrows(IllegalArgumentException.class, () -> productService.getProduct(null));
    }

    @Test
    void getProduct_shouldThrowProductNotFoundExceptionIfIdNotFound() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        assertThrows(ProductNotFoundException.class, () -> productService.getProduct("prod-not-found"));
    }

    @Test
    void getProduct_shouldGetProduct() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        assertNotNull(productService.getProduct("prod-test"));
    }

    @Test
    void getProductValid_shouldThrowProductNotFoundExceptionIfProductInactive() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        assertThrows(ProductNotFoundException.class, () -> productService.getProductIsValid("prod-inactive"));
    }

    @Test
    void validateProductRoleWithoutRole() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING);
        assertThrows(IllegalArgumentException.class, () -> productService.validateProductRole("prod-test", "productRole", null));
    }


    @Test
    void validateProductRoleOk() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING_WITH_ROLEMAPPING);
        assertDoesNotThrow(() -> productService.validateProductRole("prod-test", "operatore", PartyRole.MANAGER));
    }


    @Test
    void validateProductRoleWithProductRoleMappingNotFound() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING_WITH_ROLEMAPPING);
        assertThrows(IllegalArgumentException.class, () -> productService.validateProductRole("prod-test", "productRole", PartyRole.DELEGATE));
    }


    @Test
    void validateProductRoleOkWithProductRoleNotFound() throws JsonProcessingException {
        ProductServiceDefault productService = new ProductServiceDefault(PRODUCT_JSON_STRING_WITH_ROLEMAPPING);
        assertThrows(IllegalArgumentException.class, () -> productService.validateProductRole("prod-test", "amministratore", PartyRole.MANAGER));
    }
}
