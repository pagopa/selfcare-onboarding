package it.pagopa.selfcare.product.entity;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProductTest {

    ProductRoleInfo dummmyProductRoleInfo(PartyRole partyRole) {
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        ProductRole productRole = new ProductRole();
        productRole.setCode(partyRole.name());
        productRoleInfo.setRoles(List.of(productRole));
        return productRoleInfo;
    }

    @Test
    @DisplayName("Test when only roleMappings is non-null")
    public void testGetAllRoleMappings_OnlyRoleMappings() {
        Map<PartyRole, ProductRoleInfo> roleMappings = new HashMap<>();
        ProductRoleInfo manager = dummmyProductRoleInfo(PartyRole.MANAGER);
        ProductRoleInfo operator = dummmyProductRoleInfo(PartyRole.OPERATOR);
        roleMappings.put(PartyRole.MANAGER, manager);
        roleMappings.put(PartyRole.OPERATOR, operator);

        Product product = new Product();
        product.setRoleMappings(roleMappings);
        product.setRoleMappingsByInstitutionType(null);

        Map<PartyRole, List<ProductRoleInfo>> result = product.getAllRoleMappings();

        assertEquals(2, result.size(), "Map should contain 2 keys");
        assertEquals(List.of(manager), result.get(PartyRole.MANAGER));
        assertEquals(List.of(operator), result.get(PartyRole.OPERATOR));
    }

    @Test
    @DisplayName("Test when only roleMappingsByInstitutionType is non-null")
    public void testGetAllRoleMappings_OnlyRoleMappingsByInstitutionType() {
        Map<String, Map<PartyRole, ProductRoleInfo>> roleMappingsByInstitutionType = new HashMap<>();

        Map<PartyRole, ProductRoleInfo> institution1 = new HashMap<>();
        institution1.put(PartyRole.MANAGER, dummmyProductRoleInfo(PartyRole.MANAGER));
        institution1.put(PartyRole.OPERATOR, dummmyProductRoleInfo(PartyRole.OPERATOR));

        Map<PartyRole, ProductRoleInfo> institution2 = new HashMap<>();
        institution2.put(PartyRole.DELEGATE, dummmyProductRoleInfo(PartyRole.DELEGATE));
        institution2.put(PartyRole.OPERATOR, dummmyProductRoleInfo(PartyRole.OPERATOR));

        roleMappingsByInstitutionType.put("Institution1", institution1);
        roleMappingsByInstitutionType.put("Institution2", institution2);

        Product product = new Product();
        product.setRoleMappings(null);
        product.setRoleMappingsByInstitutionType(roleMappingsByInstitutionType);

        Map<PartyRole, List<ProductRoleInfo>> result = product.getAllRoleMappings();

        assertEquals(3, result.size(), "Map should contain 3 keys");
        assertEquals(List.of(dummmyProductRoleInfo(PartyRole.MANAGER)), result.get(PartyRole.MANAGER));
        assertEquals(List.of(dummmyProductRoleInfo(PartyRole.DELEGATE)), result.get(PartyRole.DELEGATE));
        assertEquals(Arrays.asList(
                dummmyProductRoleInfo(PartyRole.OPERATOR),
                dummmyProductRoleInfo(PartyRole.OPERATOR)
        ), result.get(PartyRole.OPERATOR));
    }

    @Test
    @DisplayName("Test when both maps are non-null with overlapping keys")
    public void testGetAllRoleMappings_BothMapsNonNullWithOverlap() {
        // Setup roleMappings
        Map<PartyRole, ProductRoleInfo> roleMappings = new HashMap<>();
        roleMappings.put(PartyRole.MANAGER, dummmyProductRoleInfo(PartyRole.MANAGER));
        roleMappings.put(PartyRole.DELEGATE, dummmyProductRoleInfo(PartyRole.OPERATOR));

        // Setup roleMappingsByInstitutionType
        Map<String, Map<PartyRole, ProductRoleInfo>> roleMappingsByInstitutionType = new HashMap<>();

        Map<PartyRole, ProductRoleInfo> institution1 = new HashMap<>();
        institution1.put(PartyRole.MANAGER, dummmyProductRoleInfo(PartyRole.MANAGER));
        institution1.put(PartyRole.OPERATOR, dummmyProductRoleInfo(PartyRole.OPERATOR));

        Map<PartyRole, ProductRoleInfo> institution2 = new HashMap<>();
        institution2.put(PartyRole.DELEGATE, dummmyProductRoleInfo(PartyRole.DELEGATE));
        institution2.put(PartyRole.OPERATOR, dummmyProductRoleInfo(PartyRole.OPERATOR));

        roleMappingsByInstitutionType.put("Institution1", institution1);
        roleMappingsByInstitutionType.put("Institution2", institution2);

        Product product = new Product();
        product.setRoleMappings(roleMappings);
        product.setRoleMappingsByInstitutionType(roleMappingsByInstitutionType);

        Map<PartyRole, List<ProductRoleInfo>> result = product.getAllRoleMappings();

        assertEquals(3, result.size(), "Map should contain 3 keys");

        // Verify MANAGER
        List<ProductRoleInfo> adminList = result.get(PartyRole.MANAGER);
        assertNotNull(adminList, "List for MANAGER should not be null");
        assertEquals(2, adminList.size(), "List for MANAGER should contain 2 elements");
        assertTrue(adminList.contains(dummmyProductRoleInfo(PartyRole.MANAGER)));
        assertTrue(adminList.contains(dummmyProductRoleInfo(PartyRole.MANAGER)));

        // Verify DELEGATE
        List<ProductRoleInfo> userList = result.get(PartyRole.DELEGATE);
        assertNotNull(userList, "List for DELEGATE should not be null");
        assertEquals(2, userList.size(), "List for DELEGATE should contain 2 elements");
        assertTrue(userList.contains(dummmyProductRoleInfo(PartyRole.DELEGATE)));
        assertTrue(userList.contains(dummmyProductRoleInfo(PartyRole.DELEGATE)));

        // Verify OPERATOR
        List<ProductRoleInfo> guestList = result.get(PartyRole.OPERATOR);
        assertNotNull(guestList, "List for OPERATOR should not be null");
        assertEquals(2, guestList.size(), "List for OPERATOR should contain 2 elements");
        assertTrue(guestList.contains(dummmyProductRoleInfo(PartyRole.OPERATOR)));
        assertTrue(guestList.contains(dummmyProductRoleInfo(PartyRole.OPERATOR)));
    }

}
