package it.pagopa.selfcare.onboarding.security;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;

@ApplicationScoped
public class JWTSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

  @Override
  public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
//    Optional<Credential> tokenCredential = identity.getCredentials().stream()
//      .filter(credential -> credential instanceof TokenCredential).findFirst();
//    Optional.of(tokenCredential).ifPresent(token -> {
//      if (token.isPresent()) {
//        try {
//          jwtValidator.p(((TokenCredential) token.get()).getToken());
//        } catch (Exception e) {
//          throw new RuntimeException(e);
//        }
//      }
//    });
//    if (identity.isAnonymous()) {
//      return Uni.createFrom().item(identity);
//    }
//    String token = extractTokenFromContext(context);
//    JsonWebToken validatedJwt = validator.validateAndParse(identity.getPrincipal().get);

    //this.jwtValidator.validateToken(identity.getCredential(JsonWebTokenCredential.class).getToken())
    if (identity.getPrincipal() instanceof JsonWebToken) {
      return Uni.createFrom().item((JsonWebToken) identity.getPrincipal())
        .onItem().transform(JsonWebToken::getIssuer)
        .onItem().transform(issuer -> {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
            builder.addRole("SUPPORT");
            builder.addAttribute("jwt.issuer", issuer);
            return builder.build();
        });
    }

    return Uni.createFrom().item(identity);
  }

  private Set<String> determineRolesForIssuer(JsonWebToken jwt, String issuer) {
    Set<String> roles = Set.of();

    switch (issuer) {
      case "SPID":
        roles = Set.copyOf(jwt.getClaimNames());
        break;
      case "PAGOPA":
        roles = Set.of("SUPPORT");
        break;
      default:
        roles = jwt.getGroups();
    }

    return roles != null ? roles : Set.of();
  }
}
