package it.pagopa.selfcare.onboarding.repository;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.utils.QueryUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class OnboardingRepository implements PanacheMongoRepositoryBase<Onboarding, String> {

    public List<Onboarding> findByFilters(String taxCode, String subunitCode, String origin, String originId, String productId) {
        final Map<String, String> queryParameter =
                QueryUtils.createMapForInstitutionOnboardingsQueryParameter(
                        taxCode, subunitCode, origin, originId, OnboardingStatus.COMPLETED, productId);
        Document query = QueryUtils.buildQuery(queryParameter);
        return find(query).list();
    }
}
