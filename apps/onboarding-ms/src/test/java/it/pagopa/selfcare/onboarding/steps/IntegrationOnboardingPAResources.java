package it.pagopa.selfcare.onboarding.steps;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class IntegrationOnboardingPAResources {
    private final Map<String, String> jsonTemplates = new HashMap<>();

    @PostConstruct
    public void init() {
        loadJsonTemplatesFromDirectory("integration_data");
    }

    private void loadJsonTemplatesFromDirectory(String directoryPath) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();

            URL resourceDirectory = classLoader.getResource(directoryPath);
            if (resourceDirectory == null) {
                System.err.println("Directory delle risorse non trovata: " + directoryPath);
                return;
            }

            File directory = new File(resourceDirectory.toURI());
            if (directory.isDirectory()) {
                for (File file : directory.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        final String templateName = file.getName().replace(".json", "");
                        String content = Files.readString(file.toPath());
                        jsonTemplates.put(templateName, content);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Errore nel caricamento dei template JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getJsonTemplate(String templateName) {
        return jsonTemplates.get(templateName);
    }
    
}