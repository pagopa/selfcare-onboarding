package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;

import java.util.Objects;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;

public class WrapperGPS extends WrapperIPA {

    public WrapperGPS(Onboarding onboarding, InstitutionApi institutionApi, UoApi uoApi) {
        super(onboarding, institutionApi, uoApi);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return super.customValidation(product).onItem().transformToUni(unused-> {
            if (PROD_PAGOPA.getValue().equals(onboarding.getProductId()) && Objects.isNull(onboarding.getAdditionalInformations())) {
                return Uni.createFrom().failure(new InvalidRequestException(ADDITIONAL_INFORMATION_REQUIRED));
            } else if (!onboarding.getAdditionalInformations().isIpa() &&
                    !onboarding.getAdditionalInformations().isBelongRegulatedMarket() &&
                    !onboarding.getAdditionalInformations().isEstablishedByRegulatoryProvision() &&
                    !onboarding.getAdditionalInformations().isAgentOfPublicService() &&
                    Objects.isNull(onboarding.getAdditionalInformations().getOtherNote())) {
                return Uni.createFrom().failure(new InvalidRequestException(OTHER_NOTE_REQUIRED));
            }
            return Uni.createFrom().item(onboarding);
        });
    }
}