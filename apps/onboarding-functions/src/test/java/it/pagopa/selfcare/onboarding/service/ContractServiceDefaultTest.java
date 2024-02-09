package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.config.AzureStorageConfig;
import it.pagopa.selfcare.onboarding.config.PagoPaSignatureConfig;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class ContractServiceDefaultTest {

    @Inject
    AzureStorageConfig azureStorageConfig;

    @InjectMock
    AzureBlobClient azureBlobClient;
    PadesSignService padesSignService;

    @Inject
    ContractService contractService;

    @Inject
    PagoPaSignatureConfig pagoPaSignatureConfig;


    final static String productNameExample = "product-name";

    @BeforeEach
    void setup(){
        padesSignService = mock(PadesSignService.class);
        contractService = new ContractServiceDefault(azureStorageConfig, azureBlobClient, padesSignService, pagoPaSignatureConfig, "logo- path", true);
    }


    private Onboarding createOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(ObjectId.get());
        onboarding.setOnboardingId("example");
        onboarding.setProductId("productId");
        onboarding.setUsers(List.of());

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PSP);
        institution.setDescription("42");

        institution.setRea("rea");
        institution.setBusinessRegisterPlace("place");
        institution.setShareCapital("10000");
        onboarding.setInstitution(institution);

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUserMailUuid("setUserMailUuid");
        onboarding.setUsers(List.of(user));
        return onboarding;
    }

    UserResource createDummyUserResource(String id, String userMailUuid) {
        UserResource validManager = new UserResource();
        validManager.setId(UUID.fromString(id));
        CertifiableFieldResourceOfstring emailCert = new CertifiableFieldResourceOfstring();
        emailCert.setValue("email");
        WorkContactResource workContact = new WorkContactResource();
        workContact.setEmail(emailCert);
        Map<String, WorkContactResource> map = new HashMap<>();
        map.put(userMailUuid, workContact);

        validManager.setWorkContacts(map);
        return validManager;
    }

    @Test
    void createContractPDF() {
        final String contractFilepath = "contract";
        final String contractHtml = "contract";
        final String productNameAccent = "Interoperabilità";

        Onboarding onboarding = createOnboarding();
        User userManager = onboarding.getUsers().get(0);
        UserResource manager = createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());

        Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

        Mockito.when(azureBlobClient.uploadFile(any(),any(),any())).thenReturn(contractHtml);

        File contract = contractService.createContractPDF(contractFilepath, onboarding, manager, List.of(), productNameAccent);

        assertNotNull(contract);

        ArgumentCaptor<String> captorFilename = ArgumentCaptor.forClass(String.class);
        verify(azureBlobClient, times(1))
                .uploadFile(any(),captorFilename.capture(),any());
        assertEquals("Interoperabilita_accordo_adesione.pdf", captorFilename.getValue());
    }

    @Test
    void createContractPDFSA() {
        final String contractFilepath = "contract";
        final String contractHtml = "contract";

        Onboarding onboarding = createOnboarding();
        User userManager = onboarding.getUsers().get(0);
        UserResource manager = createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
        onboarding.getInstitution().setInstitutionType(InstitutionType.SA);

        Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

        Mockito.when(azureBlobClient.uploadFile(any(),any(),any())).thenReturn(contractHtml);

        assertNotNull(contractService.createContractPDF(contractFilepath, onboarding, manager, List.of(), productNameExample));
    }

    @Test
    void createContractPDFAndSigned() {
        final String contractFilepath = "contract";
        final String contractHtml = "contract";

        Onboarding onboarding = createOnboarding();
        User userManager = onboarding.getUsers().get(0);
        UserResource manager = createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());

        PagoPaSignatureConfig pagoPaSignatureConfig = Mockito.spy(this.pagoPaSignatureConfig);
        when(pagoPaSignatureConfig.source()).thenReturn("local");
        contractService = new ContractServiceDefault(azureStorageConfig, azureBlobClient, padesSignService, pagoPaSignatureConfig, "logo-path", true);

        Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

        Mockito.doNothing().when(padesSignService).padesSign(any(),any(),any());

        Mockito.when(azureBlobClient.uploadFile(any(),any(),any())).thenReturn(contractHtml);

        assertNotNull(contractService.createContractPDF(contractFilepath, onboarding, manager, List.of(), productNameExample));
    }



    @Test
    void loadContractPDF() {
        final String contractFilepath = "contract";
        final String contractHtml = "contract";

        Onboarding onboarding = createOnboarding();

        File pdf = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getFile());

        Mockito.when(azureBlobClient.getFileAsPdf(contractFilepath)).thenReturn(pdf);

        Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

        assertNotNull(contractService.loadContractPDF(contractFilepath, onboarding.getId().toHexString(), productNameExample));
    }

    @Test
    void retrieveContractNotSigned() {

        Onboarding onboarding = createOnboarding();

        File pdf = mock(File.class);
        Mockito.when(azureBlobClient.getFileAsPdf(any())).thenReturn(pdf);

        contractService.retrieveContractNotSigned(onboarding.getOnboardingId(), productNameExample);

        ArgumentCaptor<String> filepathActual = ArgumentCaptor.forClass(String.class);
        Mockito.verify(azureBlobClient, times(1))
                .getFileAsPdf(filepathActual.capture());
        assertTrue(filepathActual.getValue().contains(onboarding.getOnboardingId()));
        assertTrue(filepathActual.getValue().contains(productNameExample));
    }


    @Test
    void getLogoFile() {
        Mockito.when(azureBlobClient.getFileAsText(any())).thenReturn("example");

        contractService.getLogoFile();

        Mockito.verify(azureBlobClient, times(1))
                .getFileAsText(any());
    }
}
