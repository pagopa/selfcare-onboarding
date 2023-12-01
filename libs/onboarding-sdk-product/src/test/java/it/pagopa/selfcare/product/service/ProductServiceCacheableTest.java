package it.pagopa.selfcare.product.service;

import com.azure.storage.blob.models.BlobProperties;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.exception.InvalidRoleMappingException;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//@ExtendWith({Moc.class})
class ProductServiceCacheableTest {
    final private String PRODUCT_JSON_STRING = "[{\"id\":\"prod-test-parent\",\"status\":\"ACTIVE\"}," +
            "{\"id\":\"prod-test\", \"parentId\":\"prod-test-parent\",\"status\":\"ACTIVE\"}," +
            "{\"id\":\"prod-inactive\",\"status\":\"INACTIVE\"}]";

    final private String PRODUCT_JSON_STRING_EMPTY = "[]";

    @Test
    void constructProduct() {
        //given


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        //when
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        //then
        assertNotNull(productServiceCacheable.productLastModifiedDate);
        verify(azureBlobClient, times(1)).getFileAsText(filePath);
    }

    @Test
    void createProduct_notFound() {
        //given

        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING_EMPTY);
        assertThrows(ProductNotFoundException.class, () -> new ProductServiceDefault(PRODUCT_JSON_STRING_EMPTY));

    }

    @Test
    void getProducts_rootOnly() {
        //given


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        //when
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        //then
        assertEquals(2, productServiceCacheable.getProducts(true, false).size());

    }

    @Test
    void getProducts_getAll() {
        //given


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        //when
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        //then
        assertEquals(3, productServiceCacheable.getProducts(false, false).size());
    }

    @Test
    void getProducts_rootAndValidOnly() {
        //given


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        //when
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        //then
        assertEquals(1, productServiceCacheable.getProducts(true, true).size());

    }

    @Test
    void getProduct_allValid() {
        //given


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        //when
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        //then
        assertEquals(2, productServiceCacheable.getProducts(false, true).size());
    }

    @Test
    void validateRoleMappings_exceptionNullroleMapping() {
        //given


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        //when
        Executable executable = () -> productServiceCacheable.validateRoleMappings(new HashMap<>());
        //then
        assertThrows(IllegalArgumentException.class, executable);
    }

    @Test
    void validateRoleMappings_exceptionNullRoleInfo() {
        //given


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        Map<PartyRole, ProductRoleInfo> roleMappings = new HashMap<>();
        roleMappings.put(PartyRole.MANAGER, null);
        //when
        Executable executable = () -> productServiceCacheable.validateRoleMappings(roleMappings);
        //then
        assertThrows(IllegalArgumentException.class, executable);
    }

    @Test
    void validateRoleMappings_exceptionEmptyRoleInfo() {
        //given


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        Map<PartyRole, ProductRoleInfo> roleMappings = new HashMap<>();
        roleMappings.put(PartyRole.MANAGER, new ProductRoleInfo());

        //when
        Executable executable = () -> productServiceCacheable.validateRoleMappings(roleMappings);
        //then
        assertThrows(IllegalArgumentException.class, executable);
    }

    @Test
    void validateRoleMappings_exceptionEmptyRoleInfoList() {
        //given


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        Map<PartyRole, ProductRoleInfo> roleMappings = new HashMap<>();
        ProductRoleInfo productRoleInfo1 = new ProductRoleInfo();
        productRoleInfo1.setRoles(List.of(new ProductRole(), new ProductRole()));
        roleMappings.put(PartyRole.MANAGER, productRoleInfo1);

        //when
        Executable executable = () -> productServiceCacheable.validateRoleMappings(roleMappings);
        //then
        assertThrows(InvalidRoleMappingException.class, executable);
    }

    @Test
    void getProduct_nullId() {


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        //when
        Executable executable = () -> productServiceCacheable.getProduct(null);
        //then
        assertThrows(IllegalArgumentException.class, executable);
    }

    @Test
    void getProduct_notFound() {


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        //when
        Executable executable = () -> productServiceCacheable.getProduct("notFound");
        //then
        assertThrows(ProductNotFoundException.class, executable);
    }

    @Test
    void getProduct() {


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        //when
        Product product = productServiceCacheable.getProduct("prod-test");
        //then
        assertNotNull(product);
    }

    @Test
    void getProduct_notFoundInactive() {


        final String filePath = "filePath";
        AzureBlobClient azureBlobClient = mock(AzureBlobClient.class);
        BlobProperties blobPropertiesMock = mock(BlobProperties.class);

        when(azureBlobClient.getProperties(any())).thenReturn(blobPropertiesMock);
        when(blobPropertiesMock.getLastModified()).thenReturn(OffsetDateTime.now());
        when(azureBlobClient.getFileAsText(any())).thenReturn(PRODUCT_JSON_STRING);
        ProductServiceCacheable productServiceCacheable = new ProductServiceCacheable(azureBlobClient, filePath);
        //when
        Executable executable = () -> productServiceCacheable.getProductIsValid("prod-inactive");
        //then
        assertThrows(ProductNotFoundException.class, executable);
    }

}