package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.IPAEntity;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;

public class RegistryManagerIPA extends RegistryManagerIPAUo {

    public RegistryManagerIPA(Onboarding onboarding, UoApi uoApi) {
        super(onboarding, uoApi);
    }

    @Override
    public IPAEntity retrieveInstitution() {
        super.originIdEC = onboarding.getInstitution().getOriginId();
        return IPAEntity.builder().build();
    }

    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }

}
