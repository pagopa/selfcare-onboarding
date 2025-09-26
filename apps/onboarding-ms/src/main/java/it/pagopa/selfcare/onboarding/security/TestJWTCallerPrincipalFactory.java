package it.pagopa.selfcare.onboarding.security;

import io.smallrye.jwt.auth.principal.*;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@ApplicationScoped
@Alternative
@Priority(1)
public class TestJWTCallerPrincipalFactory extends JWTCallerPrincipalFactory {
  @ConfigProperty(name = "mp.jwt.verify.publickey")
  String pubKey;

  @Inject
  JWTParser jwtParser;

  @Override
  public JWTCallerPrincipal parse(String token, JWTAuthContextInfo authContextInfo) throws ParseException {
    try {
      // Token has already been verified, parse the token claims only
      String json = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
      authContextInfo.setPublicVerificationKey(createPublicKeyFromString(pubKey, "RSA"));
      jwtParser.parse(token, authContextInfo);
      return new DefaultJWTCallerPrincipal(JwtClaims.parse(json));
    } catch (InvalidJwtException ex) {
      throw new ParseException(ex.getMessage());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static PublicKey createPublicKeyFromString(String publicKeyString, String algorithm) throws Exception {

    // 1. Pulizia: Rimuovi header, footer e interruzioni di riga.
    String publicKeyPEM = publicKeyString
      .replace("-----BEGIN PUBLIC KEY-----", "")
      .replace("-----END PUBLIC KEY-----", "")
      .replaceAll("\\s", ""); // Rimuovi tutti gli spazi bianchi

    // 2. Decodifica: Decodifica Base64 della chiave.
    byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);

    // 3. Generazione: Utilizza KeyFactory e X509EncodedKeySpec.
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
    KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

    return keyFactory.generatePublic(keySpec);
  }
}
