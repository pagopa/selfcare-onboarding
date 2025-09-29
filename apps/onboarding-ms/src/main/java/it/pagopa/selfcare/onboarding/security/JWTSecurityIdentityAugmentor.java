package it.pagopa.selfcare.onboarding.security;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
public class JWTSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

  @Override
  public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
    return Uni.createFrom().item(identity)
      .onItem().transformToUni(securityIdentity ->
        securityIdentity.getPrincipal() instanceof JsonWebToken
          ? Uni.createFrom().item((JsonWebToken) securityIdentity.getPrincipal())
          : Uni.createFrom().nullItem()
      )
      .onItem().ifNotNull().transform(JsonWebToken::getIssuer)
      .onItem().ifNotNull().transform(issuer -> QuarkusSecurityIdentity.builder(identity)
        .addRole("SUPPORT")
        .addAttribute("jwt.issuer", issuer)
        .build())
      .onItem().ifNull().fail().replaceWith(identity);
  }

//  private Set<String> determineRolesForIssuer(JsonWebToken jwt, String issuer) {
//    Set<String> roles = Set.of();
//
//    switch (issuer) {
//      case "SPID":
//        roles = Set.copyOf(jwt.getClaimNames());
//        break;
//      case "PAGOPA":
//        roles = Set.of("SUPPORT");
//        break;
//      default:
//        roles = jwt.getGroups();
//    }
//
//    return roles != null ? roles : Set.of();
//  }
}
