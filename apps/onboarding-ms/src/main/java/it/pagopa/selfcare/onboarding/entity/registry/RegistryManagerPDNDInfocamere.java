package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.entity.registry.client.ClientRegistryPDNDInfocamere;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.model.PDNDBusinessResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.SaveUserDto;
import org.openapi.quarkus.user_registry_json.model.UserSearchDto;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IDPAY_MERCHANT;

public class RegistryManagerPDNDInfocamere extends ClientRegistryPDNDInfocamere {

    public static final String USERS_FIELD_TAXCODE = "fiscalCode";

    private final UserApi userRegistryApi;
    private final Optional<String> allowedAtecoCodes;

    public RegistryManagerPDNDInfocamere(Onboarding onboarding, InfocamerePdndApi infocamerePdndApi, UserApi userRegistryApi,
                                        @ConfigProperty(name = "onboarding-ms.allowed-ateco-codes") Optional<String> allowedAtecoCodes) {
        super(onboarding, infocamerePdndApi);
        this.userRegistryApi = userRegistryApi;
        this.allowedAtecoCodes = allowedAtecoCodes;
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        if (isIdPayMerchantProduct(product)) {
            return validateAtecoCodes();
        }
        if (isPrivatePersonInstitution()) {
            return manageTaxCode();
        }
        return Uni.createFrom().item(onboarding);
    }

    private boolean isIdPayMerchantProduct(Product product) {
        return Objects.nonNull(product) && PROD_IDPAY_MERCHANT.getValue().equals(product.getId());
    }

    private boolean isPrivatePersonInstitution() {
        return InstitutionType.PRV_PF.equals(onboarding.getInstitution().getInstitutionType());
    }

    private Uni<Onboarding> handleIdPayMerchantProduct() {
        if (isPrivatePersonInstitution()) {
            return manageTaxCode();
        }
        return Uni.createFrom().item(onboarding);
    }

    private Uni<Onboarding> manageTaxCode() {
        final String fiscalCode = onboarding.getInstitution().getTaxCode();
        final UserSearchDto searchDto = new UserSearchDto().fiscalCode(fiscalCode);
        return userRegistryApi.searchUsingPOST(USERS_FIELD_TAXCODE, searchDto)
                .onItem().transform(userResource ->
                        updateOnboardingWithUserId(onboarding, userResource.getId().toString())
                )
                .onFailure(WebApplicationException.class)
                .recoverWithUni(ex ->
                        handleSearchFailure((WebApplicationException) ex, onboarding, fiscalCode)
                );
    }

    /**
     * Gestisce il fallimento della ricerca utente. Se l'errore è un 404 (Not Found),
     * procede con la creazione di un nuovo utente. Altrimenti, propaga l'errore.
     */
    private Uni<Onboarding> handleSearchFailure(WebApplicationException ex, Onboarding onboarding, String fiscalCode) {
        if (ex.getResponse().getStatus() == 404) {
            return createNewUserAndUpdateOnboarding(onboarding, fiscalCode);
        }
        return Uni.createFrom().failure(ex);
    }

    /**
     * Crea un nuovo utente tramite API e aggiorna l'oggetto onboarding con l'ID restituito.
     */
    private Uni<Onboarding> createNewUserAndUpdateOnboarding(Onboarding onboarding, String fiscalCode) {
        final SaveUserDto saveUserDto = new SaveUserDto();
        saveUserDto.setFiscalCode(fiscalCode);
        return userRegistryApi.saveUsingPATCH(saveUserDto)
                .onItem()
                .transform(userId ->
                        updateOnboardingWithUserId(onboarding, userId.getId().toString())
                );
    }

    /**
     * Metodo di utilità per aggiornare l'oggetto onboarding con un ID utente,
     * eliminando la duplicazione del codice.
     */
    private Onboarding updateOnboardingWithUserId(Onboarding onboarding, String userId) {
        onboarding.getInstitution().setTaxCode(userId);
        onboarding.getInstitution().setOriginId(userId);
        return onboarding;
    }

    /**
     * Valida che almeno uno dei codici ATECO dell'istituzione sia presente nella lista di codici ATECO consentiti.
     * @return Uni<Void> che fallisce con InvalidRequestException se la validazione fallisce
     */
    private Uni<Onboarding> validateAtecoCodes() {
        List<String> institutionAtecoCodes = onboarding.getInstitution().getAtecoCodes();
        
        if (Objects.isNull(institutionAtecoCodes) || institutionAtecoCodes.isEmpty()) {
            return Uni.createFrom().failure(new InvalidRequestException("Institution must have at least one ATECO code"));
        }

        List<String> allowedCodes = allowedAtecoCodes
                .map(codes -> List.of(codes.split(",")))
                .orElse(List.of());
        boolean hasValidAteco = institutionAtecoCodes.stream()
                .anyMatch(ateco -> allowedCodes.contains(ateco.trim()));

        if (!hasValidAteco) {
            return Uni.createFrom().failure(new InvalidRequestException("Institution ATECO codes are not allowed for this product"));
        }

        return Uni.createFrom().item(onboarding);
    }

    @Override
    public Uni<Boolean> isValid() {
        /*if (!originPDNDInfocamere(onboarding, registryResource)) {
              return Uni.createFrom().failure(new InvalidRequestException("Field digitalAddress or description are not valid"));
        }*/
        return Uni.createFrom().item(true);
    }

    private boolean originPDNDInfocamere(Onboarding onboarding, PDNDBusinessResource pdndBusinessResource) {
        return onboarding.getInstitution().getDigitalAddress().equals(pdndBusinessResource.getDigitalAddress()) &&
                onboarding.getInstitution().getDescription().equals(pdndBusinessResource.getBusinessName());
    }

}