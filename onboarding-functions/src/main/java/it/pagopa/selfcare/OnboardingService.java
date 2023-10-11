package it.pagopa.selfcare;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.entity.User;
import it.pagopa.selfcare.exception.GenericOnboardingException;
import it.pagopa.selfcare.repository.OnboardingRepository;
import it.pagopa.selfcare.utils.GenericError;
import it.pagopa.selfcare.service.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ApplicationScoped
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    @Inject
    NotificationService notificationService;

    @Inject
    OnboardingRepository repository;
    public Onboarding getOnboarding(String onboardingId) {
        return repository.findById(new ObjectId(onboardingId));
    }

    public void createContract(Onboarding onboarding) {
        String validManagerId = getValidManagerId(onboarding.getUsers());
        //User manager = userService.retrieveUserFromUserRegistry(validManagerId, EnumSet.allOf(User.Fields.class));

        //List<User> delegates = onboarding.getUsers()
         //       .stream()
         //       .filter(userToOnboard -> PartyRole.MANAGER != userToOnboard.getRole())
         //       .map(userToOnboard -> userService.retrieveUserFromUserRegistry(userToOnboard.getId(), EnumSet.allOf(User.Fields.class))).collect(Collectors.toList());

        //String contractTemplate = contractService.extractTemplate(strategyInput.getOnboardingRequest().getContract().getPath());

        //File pdf = contractService.createContractPDF(contractTemplate, manager, delegates, onboarding.getInstitution(), strategyInput.getOnboardingRequest(), strategyInput.getInstitutionUpdateGeographicTaxonomies(), strategyInput.getOnboardingRequest().getInstitutionUpdate().getInstitutionType());

    }

    public void loadContract(Onboarding onboarding) {

        //File pdf = fileStorageConnector.getFileAsPdf(strategyInput.getOnboardingRequest().getContract().getPath());

    }

    public String getValidManagerId(List<User> users) {
        log.debug("START - getOnboardingValidManager for users list size: {}", users.size());

        return users.stream()
                .filter(userToOnboard -> PartyRole.MANAGER == userToOnboard.getRole())
                .map(User::getId)
                .findAny()
                .orElseThrow(() -> new GenericOnboardingException(GenericError.MANAGER_NOT_FOUND_GENERIC_ERROR.getMessage(),
                        GenericError.MANAGER_NOT_FOUND_GENERIC_ERROR.getCode()));
    }
}
