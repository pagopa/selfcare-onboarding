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

        List<Product> productList = mapper.readValue(productString, new TypeReference<List<Product>>(){});
        if(Objects.isNull(productList) || productList.isEmpty()) throw new ProductNotFoundException("json string is empty!");
        this.productsMap = productList.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    public ProductServiceDefault(String productString, ObjectMapper mapper) throws JsonProcessingException {

        List<Product> productList = mapper.readValue(productString, new TypeReference<List<Product>>(){});
        if(Objects.isNull(productList) || productList.isEmpty()) throw new ProductNotFoundException("json string is empty!");
        this.productsMap = productList.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    @Override
    public List<Product> getProducts(boolean rootOnly) {

        return rootOnly
            ? productsMap.values().stream()
                .filter(product -> Objects.nonNull(product.getParentId()))
                .collect(Collectors.toList())
            :  productsMap.values().stream()
                .filter(product -> !ProductStatus.INACTIVE.equals(product.getStatus()))
                .collect(Collectors.toList());
    }


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


    @Override
    public Product getProduct(String id, InstitutionType institutionType) {

       if(Objects.isNull(id))
           throw new IllegalArgumentException(REQUIRED_PRODUCT_ID_MESSAGE);
        Product foundProduct = Optional.ofNullable(productsMap.get(id)).orElseThrow(ProductNotFoundException::new);
        if (foundProduct.getStatus() == ProductStatus.INACTIVE) {
            throw new ProductNotFoundException();
        }
        if (institutionType != null && foundProduct.getInstitutionContractMappings() != null && foundProduct.getInstitutionContractMappings().containsKey(institutionType)) {
            foundProduct.setContractTemplatePath(foundProduct.getInstitutionContractMappings().get(institutionType).getContractTemplatePath());
            foundProduct.setContractTemplateVersion(foundProduct.getInstitutionContractMappings().get(institutionType).getContractTemplateVersion());
        }
        return foundProduct;
    }

    @Override
    public Product getProductIsValid(String id) {
        if(Objects.isNull(id))
            throw new IllegalArgumentException(REQUIRED_PRODUCT_ID_MESSAGE);
        Product foundProduct = Optional.ofNullable(productsMap.get(id)).orElseThrow(ProductNotFoundException::new);
        Product baseProduct = null;
        if (foundProduct.getParentId() != null) {
            baseProduct = Optional.ofNullable(productsMap.get(id)).orElseThrow(ProductNotFoundException::new);
            if (baseProduct.getStatus() == ProductStatus.PHASE_OUT) {
                return null;
            } else if  (foundProduct.getStatus() != ProductStatus.PHASE_OUT){
                foundProduct.setParent(baseProduct);
                return foundProduct;
            }
        } else if (foundProduct.getStatus() != ProductStatus.PHASE_OUT) {
            return foundProduct;
        }

        return null;
    }


}
