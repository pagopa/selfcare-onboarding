package it.pagopa.selfcare.onboarding.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.onboarding.util.SortEnum;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.http.HttpStatus;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.openapi.quarkus.onboarding_functions_json.api.NotificationsApi;
import org.openapi.quarkus.onboarding_functions_json.model.OnboardingStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class NotificationServiceDefault implements NotificationService {
    private static final Logger LOG = Logger.getLogger(NotificationServiceDefault.class);

    private static final int PAGE_SIZE = 100;

    @Inject
    OnboardingMapper onboardingMapper;

    @RestClient
    @Inject
    NotificationsApi notificationsApi;

    @Override
    public Uni<Void> resendOnboardingNotifications(OnboardingGetFilters filters) {
        checkFilters(filters);
        Uni.createFrom().item(filters)
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .subscribe()
                .with(this::resendNotifications);

        return Uni.createFrom().voidItem();
    }

    private void checkFilters(OnboardingGetFilters filters) {
        if(filters.getStatus() == null) {
            throw new InvalidRequestException("Status is required");
        }

        if(!OnboardingStatus.COMPLETED.name().equals(filters.getStatus()) && !OnboardingStatus.DELETED.name().equals(filters.getStatus())) {
            throw new InvalidRequestException("Status must be COMPLETED or DELETED");
        }
    }

    public void resendNotifications(OnboardingGetFilters filters) {
        LOG.infof("Resending notifications for onboarding with filters: %s", filters);

        Document sort = QueryUtils.buildSortDocument(Onboarding.Fields.createdAt.name(), SortEnum.DESC);
        Map<String, String> queryParameter = QueryUtils.createMapForOnboardingQueryParameter(filters);
        Document query = QueryUtils.buildQuery(queryParameter);

        executeResend(query, sort)
                .subscribe().with(
                        ignored -> LOG.info("Resend completed"),
                        e -> LOG.error("Error resending notifications", e)
                );
    }

    private Uni<Void> executeResend(Document query, Document sort) {
        LOG.infof("Executing resend");

        return Multi.createBy().repeating()
                .uni(
                        AtomicInteger::new,
                        currentPage -> runQueryAndSendNotification(query, sort, currentPage.getAndIncrement())
                )
                .until(countRetrievedOnboardings -> countRetrievedOnboardings == 0 || countRetrievedOnboardings != PAGE_SIZE)
                .collect()
                .asList()
                .flatMap(ignored -> Uni.createFrom().nullItem());
    }

    private Uni<Integer> runQueryAndSendNotification(Document query, Document sort, int page) {
        LOG.infof("Running query and sending notification for page %d", page);
        return runQuery(query, sort).page(page, PAGE_SIZE).stream()
                .map(onboardingMapper::mapOnboardingForNotification)
                .call(this::sendNotification)
                .collect()
                .asList()
                .map(List::size)
                .invoke(countRetrievedOnboardings -> LOG.infof("Sent %d notifications for page %d", countRetrievedOnboardings, page));

    }

    private Uni<Void> sendNotification(org.openapi.quarkus.onboarding_functions_json.model.Onboarding onboarding) {
        LOG.infof("Sending notification for onboarding with id %s", onboarding.getId());
        return notificationsApi.apiNotificationsPost(null, onboarding)
                .onFailure().invoke(e -> LOG.errorf("Error sending notification for onboarding with id %s", onboarding.getId(), e))
                .onFailure(this::shouldIgnoreException).recoverWithNull()
                .map(ignored -> null);
    }

    private boolean shouldIgnoreException(Throwable t) {
        if (t instanceof ClientWebApplicationException e) {
            return e.getResponse().getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR;
        }

        return false;
    }

    private ReactivePanacheQuery<Onboarding> runQuery(Document query, Document sort) {
        return Onboarding.find(query, sort);
    }
}
