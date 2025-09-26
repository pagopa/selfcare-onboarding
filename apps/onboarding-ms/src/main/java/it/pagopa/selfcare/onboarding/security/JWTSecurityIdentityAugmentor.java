package it.pagopa.selfcare.onboarding.security;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.smallrye.jwt.runtime.auth.JsonWebTokenCredential;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;



import java.util.Set;

@ApplicationScoped
public class JWTSecurityIdentityAugmentor implements SecurityIdentityAugmentor {
  @Inject
  MultiIssuerJWTValidator jwtValidator;

  @Override
  public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
    if (identity.isAnonymous()) {
      return Uni.createFrom().item(identity);
    }
                                                        //this.jwtValidator.validateToken(identity.getCredential(JsonWebTokenCredential.class).getToken())
    if (identity.getPrincipal() instanceof JsonWebToken) {
      return Uni.createFrom().item((JsonWebToken) identity.getPrincipal())
        .onItem().transform(JsonWebToken::getIssuer)
        .onItem().transform(issuer -> {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
            builder.addAttribute("jwt.issuer", issuer);
            builder.addAttribute("jwt.supported.issuers", jwtValidator.getSupportedIssuers());
            return builder.build();
        });


//      JsonWebToken jwt = (JsonWebToken) identity.getPrincipal();
//      String issuer = jwt.getIssuer();

//      QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
//      builder.addAttribute("jwt.issuer", issuer);
//      builder.addAttribute("jwt.supported.issuers", jwtValidator.getSupportedIssuers());

//      Set<String> issuerSpecificRoles = determineRolesForIssuer(jwt, issuer);
//      issuerSpecificRoles.forEach(builder::addRole);

//      return Uni.createFrom().item(builder.build());
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
