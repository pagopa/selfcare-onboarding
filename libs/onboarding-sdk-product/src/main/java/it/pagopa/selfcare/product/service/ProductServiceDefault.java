package it.pagopa.selfcare.product.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.entity.ProductStatus;
import it.pagopa.selfcare.product.exception.InvalidRoleMappingException;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProductServiceDefault implements ProductService {

    protected static final String REQUIRED_PRODUCT_ID_MESSAGE = "A product id is required";

    final Map<String, Product> productsMap;

    public ProductServiceDefault(String productString) throws JsonProcessingException {
        /* define object mapper */
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.productsMap = constructProductsMap(productString, mapper);
    }

    public ProductServiceDefault(String productString, ObjectMapper mapper) throws JsonProcessingException {
        this.productsMap = constructProductsMap(productString, mapper);
    }

    private Map<String, Product> constructProductsMap(String productString, ObjectMapper mapper) throws JsonProcessingException {
        List<Product> productList = mapper.readValue(productString, new TypeReference<List<Product>>(){});
        if(Objects.isNull(productList) || productList.isEmpty()) throw new ProductNotFoundException("json string is empty!");
        return productList.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    /**
     * Returns the list of PagoPA products tree which are not INACTIVE
     * @param rootOnly if true only product that has parent is null are returned
     * @return List of PagoPA products
     */
    @Override
    public List<Product> getProducts(boolean rootOnly, boolean valid) {

        return rootOnly
            ? productsMap.values().stream()
                .filter(product -> Objects.isNull(product.getParentId()))
                .filter(product -> !valid || !statusIsNotValid(product.getStatus()))
                .collect(Collectors.toList())
            : productsMap.values().stream()
                .filter(product -> !valid || !statusIsNotValid(product.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Utility method for validating role mappings that contains associations between Selfcare role and Product role.
     * Each Selfcare role must be only one Product role except OPERATOR.
     * @param roleMappings
     * @throws IllegalArgumentException roleMappings is null or empty
     * @throws InvalidRoleMappingException Selfcare role have more than one Product role
     */
    @Override
    public void validateRoleMappings(Map<PartyRole, ? extends ProductRoleInfo> roleMappings) {

        if(Objects.isNull(roleMappings) || roleMappings.isEmpty())
            throw new IllegalArgumentException("A product role mappings is required");
        roleMappings.forEach((partyRole, productRoleInfo) -> {
            if(Objects.isNull(productRoleInfo))
                throw new IllegalArgumentException("A product role info is required");
            if(Objects.isNull(productRoleInfo.getRoles()) || productRoleInfo.getRoles().isEmpty())
                throw new IllegalArgumentException("At least one Product role are required");
            if (productRoleInfo.getRoles().size() > 1 && !PartyRole.OPERATOR.equals(partyRole)) {
                throw new InvalidRoleMappingException(String.format("Only '%s' Party-role can have more than one Product-role, %s",
                        PartyRole.OPERATOR.name(),
                        String.format("partyRole = %s => productRoleInfo = %s", partyRole, productRoleInfo)));
            }
        });
    }

    /**
     * Return a product by productId without any filter
     * retrieving data from institutionContractMappings map
     * @param productId
     * @return Product
     * @throws IllegalArgumentException if @param id is null
     * @throws ProductNotFoundException if product is not found
     *
     */
    @Override
    public Product getProduct(String productId) {
        return getProduct(productId, false);
    }

    private Product getProduct(String productId, boolean filterValid) {

        if(Objects.isNull(productId)) {
            throw new IllegalArgumentException(REQUIRED_PRODUCT_ID_MESSAGE);
        }
        Product product = Optional.ofNullable(productsMap.get(productId))
                .orElseThrow(ProductNotFoundException::new);
        if (filterValid && statusIsNotValid(product.getStatus())) {
            throw new ProductNotFoundException();
        }

        if (product.getParentId() != null) {
            Product parent = Optional.ofNullable(productsMap.get(product.getParentId()))
                    .orElseThrow(ProductNotFoundException::new);
            if (filterValid && statusIsNotValid(parent.getStatus())) {
                throw new ProductNotFoundException();
            }

            product.setParent(parent);
        }

        return product;
    }

    /**
     * Fills contractTemplatePath and ContractTemplateVersion based on @param institutionType.
     * If institutionContractMappings contains institutionType, it take value from that setting inside
     * contractTemplatePath and contractTemplateVersion of product
     * @param product Product
     * @param institutionType InstitutionType
     */
    @Override
    public void fillContractTemplatePathAndVersion(Product product, InstitutionType institutionType) {
        if (institutionType != null && product.getInstitutionContractMappings() != null && product.getInstitutionContractMappings().containsKey(institutionType)) {
            product.setContractTemplatePath(product.getInstitutionContractMappings().get(institutionType).getContractTemplatePath());
            product.setContractTemplateVersion(product.getInstitutionContractMappings().get(institutionType).getContractTemplateVersion());
        }
    }


    /**
     * Returns the information for a single product if it has not PHASE_OUT,INACTIVE status and its parent has not PHASE_OUT,INACTIVE status
     * @param productId
     * @return Product if it is valid or null if it has PHASE_OUT,INACTIVE status
     * @throws IllegalArgumentException product id is null
     * @throws ProductNotFoundException product id or its parent does not exist or have PHASE_OUT,INACTIVE status
     */
    @Override
    public Product getProductIsValid(String productId) {
        return getProduct(productId, true);
    }

    /**
     * The validateProductRole function is used to validate a product role searching for it within the role map for a specific product
     * given productId. It returns founded ProductRole object filtered by productRole code
     */
    @Override
    public ProductRole validateProductRole(String productId, String productRole, PartyRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is mandatory to check productRole");
        }

        Product product = getProduct(productId);
        ProductRoleInfo productRoleInfo = product.getRoleMappings().get(role);
        if (productRoleInfo == null) {
            throw new IllegalArgumentException(String.format("Role %s not found", role));
        }

        return productRoleInfo.getRoles().stream().filter(prodRole -> prodRole.getCode().equals(productRole))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("ProductRole %s not found for role %s", productRole, role)));
    }

    private static boolean statusIsNotValid(ProductStatus status) {
        return List.of(ProductStatus.INACTIVE, ProductStatus.PHASE_OUT).contains(status);
    }


}
