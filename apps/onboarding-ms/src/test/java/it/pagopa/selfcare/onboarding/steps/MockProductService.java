package it.pagopa.selfcare.onboarding.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Alternative
@Priority(1)
@ApplicationScoped
public class MockProductService implements ProductService {

    private static final String PUBLIC_ENDPOINT = "https://raw.githubusercontent.com/pagopa/selfcare-infra-private/refs/heads/main/products/env/dev/products.json";
    private boolean initialized = false;

    private List<Product> products;
    private final CountDownLatch initLatch = new CountDownLatch(1);

    // Metodo pubblico per forzare l'inizializzazione in modo bloccante
    public void initializeBlocking() {
        if (!initialized) {
            loadProductsFromHttp();
            initialized = true;
            initLatch.countDown();
        }
    }

    // Metodo per attendere l'inizializzazione
    public void awaitInitialization() throws InterruptedException {
        initLatch.await(20, TimeUnit.SECONDS); // Timeout di 30 secondi
    }

    private void loadProductsFromHttp() {
        try {
            String githubToken = System.getenv("GITHUB_TOKEN");
            if (githubToken == null || githubToken.isEmpty()) {
               // LOG.warn("GITHUB_TOKEN non trovato nelle variabili d'ambiente");
                products = getFallbackProducts();
                return;
            }
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PUBLIC_ENDPOINT))
                    .header("Accept", "application/json")
                    .header("Authorization", "token " + githubToken)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String jsonResponse = response.body();
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());

                // Configura la serializzazione delle date come timestamp ISO-8601
                mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                products = mapper.readValue(jsonResponse,
                        mapper.getTypeFactory().constructCollectionType(List.class, Product.class));
            } else {
                products = getFallbackProducts();
            }
        } catch (Exception e) {
            products = getFallbackProducts();
        }
    }

    private List<Product> getFallbackProducts() {
        return List.of();
    }

    @Override
    public List<Product> getProducts(boolean rootOnly, boolean valid) {
        if (!initialized) {
            try {
                initializeBlocking();
            } catch (Exception e) {
                products = getFallbackProducts();
                initialized = true;
                initLatch.countDown();
            }
        }
        return products;
    }

    @Override
    public void validateRoleMappings(Map<PartyRole, ? extends ProductRoleInfo> roleMappings) {

    }

    @Override
    public Product getProduct(String productId) {
        return null;
    }

    @Override
    public Product getProductRaw(String productId) {
        return null;
    }

    @Override
    public Product getProductIsValid(String productId) {
        if (!initialized) {
            try {
                initializeBlocking();
            } catch (Exception e) {
                products = getFallbackProducts();
                initialized = true;
                initLatch.countDown();
            }
        }
    return products.stream()
        .filter(product -> product.getId().equals(productId))
        .findAny()
        .orElse(null);
    }

    @Override
    public ProductRole validateProductRole(String productId, String productRole, PartyRole role) {
        return null;
    }
}