package it.pagopa.selfcare.product.utils;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.PHASE_ADDITION_ALLOWED;
import it.pagopa.selfcare.product.entity.Product;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProductUtils {

    /**
     * Returns list of product's PartyRole associates with that PHASE_ADDITION_ALLOWED
     * @param product Product
     * @param phase phase
     * @return List<PartyRole>
     */
    public static List<PartyRole> validRoles(Product product, PHASE_ADDITION_ALLOWED phase) {
        if (Objects.isNull(product)) {
            throw new IllegalArgumentException("Product must not be null!");
        }
        return Optional.ofNullable(product.getRoleMappings())
                .orElse(Map.of())
                .entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue().getPhasesAdditionAllowed()) &&
                        entry.getValue().getPhasesAdditionAllowed().contains(phase.value))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

    }
}
