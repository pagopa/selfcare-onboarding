package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.config.AzureStorageConfig;
import it.pagopa.selfcare.onboarding.config.PagoPaSignatureConfig;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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


    @BeforeEach
    void setup(){
        padesSignService = mock(PadesSignService.class);
        contractService = new ContractServiceDefault(azureStorageConfig, azureBlobClient, padesSignService, pagoPaSignatureConfig);
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
        return onboarding;
    }

    UserResource createDummyUserResource() {
        UserResource validManager = new UserResource();
        CertifiableFieldResourceOfstring emailCert = new CertifiableFieldResourceOfstring();
        emailCert.setValue("email");
        WorkContactResource workContact = new WorkContactResource();
        workContact.setEmail(emailCert);
        Map<String, WorkContactResource> map = new HashMap<>();
        map.put("id", workContact);
        validManager.setWorkContacts(map);
        return validManager;
    }

    @Test
    void createContractPDF() {
        final String contractFilepath = "contract";
        final String contractHtml = "contract";

        UserResource manager = createDummyUserResource();
        Onboarding onboarding = createOnboarding();

        Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

        Mockito.when(azureBlobClient.uploadFile(any(),any(),any())).thenReturn(contractHtml);

        assertNotNull(contractService.createContractPDF(contractFilepath, onboarding, manager, List.of(), List.of()));
    }

    @Test
    void createContractPDFSA() {
        final String contractFilepath = "contract";
        final String contractHtml = "contract";

        UserResource manager = createDummyUserResource();
        Onboarding onboarding = createOnboarding();
        onboarding.getInstitution().setInstitutionType(InstitutionType.SA);

        Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

        Mockito.when(azureBlobClient.uploadFile(any(),any(),any())).thenReturn(contractHtml);

        assertNotNull(contractService.createContractPDF(contractFilepath, onboarding, manager, List.of(), List.of()));
    }

    @Test
    void createContractPDFAndSigned() {
        final String contractFilepath = "contract";
        final String contractHtml = "contract";

        UserResource manager = createDummyUserResource();
        Onboarding onboarding = createOnboarding();

        PagoPaSignatureConfig pagoPaSignatureConfig = Mockito.spy(this.pagoPaSignatureConfig);
        when(pagoPaSignatureConfig.source()).thenReturn("local");
        contractService = new ContractServiceDefault(azureStorageConfig, azureBlobClient, padesSignService, pagoPaSignatureConfig);

        Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

        Mockito.doNothing().when(padesSignService).padesSign(any(),any(),any());

        Mockito.when(azureBlobClient.uploadFile(any(),any(),any())).thenReturn(contractHtml);

        assertNotNull(contractService.createContractPDF(contractFilepath, onboarding, manager, List.of(), List.of()));
    }



    @Test
    void loadContractPDF() {
        final String contractFilepath = "contract";
        final String contractHtml = "contract";

        Onboarding onboarding = createOnboarding();

        File pdf = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getFile());

        Mockito.when(azureBlobClient.getFileAsPdf(contractFilepath)).thenReturn(pdf);

        Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

        assertNotNull(contractService.loadContractPDF(contractFilepath, onboarding.getId().toHexString()));
    }

    @Test
    void retrieveContractNotSigned() {

        Onboarding onboarding = createOnboarding();

        File pdf = mock(File.class);
        Mockito.when(azureBlobClient.getFileAsPdf(any())).thenReturn(pdf);

        contractService.retrieveContractNotSigned(onboarding.getOnboardingId());

        ArgumentCaptor<String> filepathActual = ArgumentCaptor.forClass(String.class);
        Mockito.verify(azureBlobClient, times(1))
                .getFileAsPdf(filepathActual.capture());
        assertTrue(filepathActual.getValue().contains(onboarding.getOnboardingId()));
    }

}
