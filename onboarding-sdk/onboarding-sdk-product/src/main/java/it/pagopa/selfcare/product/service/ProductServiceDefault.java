package it.pagopa.selfcare.product.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.commons.base.utils.InstitutionType;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.entity.ProductStatus;
import it.pagopa.selfcare.product.exception.InvalidRoleMappingException;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductServiceDefault implements ProductService {


    private static final Logger log = LoggerFactory.getLogger(ProductServiceDefault.class);

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
        log.trace("getProducts start");
        List<Product> products = rootOnly
            ? productsMap.values().stream()
                .filter(product -> Objects.nonNull(product.getParentId()))
                .collect(Collectors.toList())
            :  productsMap.values().stream()
                .filter(product -> !ProductStatus.INACTIVE.equals(product.getStatus()))
                .collect(Collectors.toList());

        log.debug("getProducts result = {}", products);
        log.trace("getProducts end");
        return products;
    }


    @Override
    public void validateRoleMappings(EnumMap<PartyRole, ? extends ProductRoleInfo> roleMappings) {
        log.trace("validateRoleMappings start");
        log.debug("validateRoleMappings roleMappings = {}", roleMappings);
        Assert.notEmpty(roleMappings, "A product role mappings is required");
        roleMappings.forEach((partyRole, productRoleInfo) -> {
            Assert.notNull(productRoleInfo, "A product role info is required");
            Assert.notEmpty(productRoleInfo.getRoles(), "At least one Product role are required");
            if (productRoleInfo.getRoles().size() > 1 && !PartyRole.OPERATOR.equals(partyRole)) {
                throw new InvalidRoleMappingException(String.format("Only '%s' Party-role can have more than one Product-role, %s",
                        PartyRole.OPERATOR.name(),
                        String.format("partyRole = %s => productRoleInfo = %s", partyRole, productRoleInfo)));
            }
        });
        log.trace("validateRoleMappings end");
    }


    @Override
    public Product getProduct(String id, InstitutionType institutionType) {
        log.trace("getProduct start");
        log.debug("getProduct id = {}", id);
        Assert.hasText(id, REQUIRED_PRODUCT_ID_MESSAGE);
        Product foundProduct = Optional.ofNullable(productsMap.get(id)).orElseThrow(ProductNotFoundException::new);
        if (foundProduct.getStatus() == ProductStatus.INACTIVE) {
            throw new ProductNotFoundException();
        }
        if (institutionType != null && foundProduct.getInstitutionContractMappings() != null && foundProduct.getInstitutionContractMappings().containsKey(institutionType)) {
            foundProduct.setContractTemplatePath(foundProduct.getInstitutionContractMappings().get(institutionType).getContractTemplatePath());
            foundProduct.setContractTemplateVersion(foundProduct.getInstitutionContractMappings().get(institutionType).getContractTemplateVersion());
        }
        log.debug("getProduct result = {}", foundProduct);
        log.trace("getProduct end");
        return foundProduct;
    }

    @Override
    public Product getProductIsValid(String id) {
        log.trace("getProduct start");
        log.debug("getProduct id = {}", id);
        Assert.hasText(id, REQUIRED_PRODUCT_ID_MESSAGE);
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

        log.debug("getProduct result = {}", foundProduct);
        log.debug("getBaseProduct result = {}", baseProduct);
        log.trace("getProduct end");
        return null;
    }


}
