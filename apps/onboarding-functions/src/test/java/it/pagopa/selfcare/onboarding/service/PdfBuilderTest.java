package it.pagopa.selfcare.onboarding.service;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static io.smallrye.common.constraint.Assert.assertTrue;

import it.pagopa.selfcare.onboarding.utils.PdfBuilder;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;

@Slf4j
class PdfBuilderTest {

    @Inject
    ContractService contractService;

    static String templateContent;
    static Map<String, Object> content;

    @BeforeAll
    static void initialize() throws IOException {
        // u need to set the local path of the template file
        templateContent =
            setupTemplateContext(
                "C:\\Users\\gferrara\\git\\selfcare-infra\\src\\core\\contracts_template\\contracts\\template\\pagopa\\1.0.0\\pagopa-accordo_di_adesione_prv-v.1.0.0.html");
        content = setupContent();
    }

    @Test
    void generateDocumentTest() throws IOException {
        // given

        // when
        File result = PdfBuilder.generateDocument("namePdf", templateContent, content);

        // then
        assertNotNull(result);
        assertTrue(result.exists());
        log.debug("Temp path: {}", result.getAbsolutePath());

        // result.deleteOnExit();
    }

    private static Map<String, Object> setupContent() {

        List<UserResource> users = new ArrayList<>();
        UserResource user = new UserResource();
        user.setName(
            new CertifiableFieldResourceOfstring()
                .value("name")
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        user.setEmail(
            new CertifiableFieldResourceOfstring()
                .value("mail@live.it")
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        user.setFamilyName(
            new CertifiableFieldResourceOfstring()
                .value("family name")
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE));
        user.setFiscalCode("24354454564564");
        users.add(user);

        HashMap<String, Object> map = new HashMap<String, Object>();
        // Added fake value
        map.put("zipCode", "84011");
        map.put("address", "via Roma");
        map.put("institutionName", "Paolo paoli");
        map.put("vatNumberGroup", "partita iva di gruppo");
        map.put("vatNumberGroupCheckbox1", "X");
        map.put("vatNumberGroupCheckbox2", "");
        //map.put("delegates", PdfMapper.delegatesToText(users, new ArrayList<>()));

        return map;
    }

    private static String setupTemplateContext(String directoryFile) throws IOException {
        Path path = Paths.get(directoryFile);

        InputStream in = Files.newInputStream(path);
        String content = StringUtils.EMPTY;

        try {
            content = IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error: ", e);
        } finally {
            IOUtils.closeQuietly(in);
        }

        return content;
    }
}