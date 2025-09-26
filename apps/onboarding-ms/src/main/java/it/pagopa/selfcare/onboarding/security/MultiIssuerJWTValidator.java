package it.pagopa.selfcare.onboarding.security;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class MultiIssuerJWTValidator {
  @Inject
  JWTParser jwtParser;



  private final Map<String, IssuerConfig> supportedIssuers = new ConcurrentHashMap<>();

  public static class IssuerConfig {
    public final String jwksUrl;
    public final String audience;

    public IssuerConfig(String jwksUrl, String audience) {
      this.jwksUrl = jwksUrl;
      this.audience = audience;
    }
  }

  public MultiIssuerJWTValidator() {
    supportedIssuers.put("SPID",
      new IssuerConfig("https://dev.selfcare.pagopa.it/.well-known/jwks.json", "api.dev.selfcare.pagopa.it"));
    supportedIssuers.put("PAGOPA",
      new IssuerConfig("https://dev.selfcare.pagopa.it/.well-known/jwks.json", "api.dev.selfcare.pagopa.it"));
  }

  public JsonWebToken validateToken(String token) {
    try {
      String[] tokenParts = token.split("\\.");
      if (tokenParts.length != 3) {
        throw new ParseException("Invalid JWT format");
      }

      String payload = new String(java.util.Base64.getUrlDecoder().decode(tokenParts[1]));
      JsonObject claims = new JsonObject(payload);
      String issuer = claims.getString("iss");

      if (issuer == null || !supportedIssuers.containsKey(issuer)) {
        throw new ParseException("Unsupported issuer: " + issuer);
      }

      return validateTokenForIssuer(token, issuer);

    } catch (Exception e) {
      throw new RuntimeException("JWT validation failed", e);
    }

  }

  private JsonWebToken validateTokenForIssuer(String token, String issuer) throws ParseException {
    IssuerConfig config = supportedIssuers.get(issuer);
    // Qui dovresti implementare la validazione specifica per l'issuer
    // Questo è un esempio semplificato - in produzione useresti JWTParser con configurazione dinamica
    return jwtParser.parse(token);
  }

  public Set<String> getSupportedIssuers() {
    return supportedIssuers.keySet();
  }
}
