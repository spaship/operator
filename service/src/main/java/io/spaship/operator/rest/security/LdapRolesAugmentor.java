package io.spaship.operator.rest.security;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.spaship.operator.ldap.LdapService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.security.Principal;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Adds Ldap Roles to Security Identity
 */
@ApplicationScoped
public class LdapRolesAugmentor implements SecurityIdentityAugmentor {

    private static final Logger log = Logger.getLogger(LdapRolesAugmentor.class);

    @Inject
    LdapService ldapService;
    @ConfigProperty(name = "app.ldap.jwt.claim")
    Optional<String> claim;

    void onStart(@Observes StartupEvent ev) {
        log.infof("LDAP Roles Identity Augmentor init. enabled=%s claim=%s", isEnabled(), claim.orElse("N/A"));
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous() || !isEnabled()) {
            return sameIdentity(identity);
        }
        Principal principal = identity.getPrincipal();
        log.tracef("principal=%s", principal);

        String filterValue = principal.getName();

        if (claim.isPresent() && principal instanceof JsonWebToken) {
            JsonWebToken token = (JsonWebToken) principal;
            log.debug("Getting filter value from claim");
            filterValue = token.getClaim(claim.get());
        }

        Set<String> roles = ldapService.getRoles(filterValue);
        if (roles.isEmpty()) {
            return sameIdentity(identity);
        }

        return Uni.createFrom().item(build(identity, roles));
    }

    public boolean isEnabled() {
        return ldapService.isEnabled();
    }

    private Uni<SecurityIdentity> sameIdentity(SecurityIdentity identity) {
        return Uni.createFrom().item(() -> identity);
    }

    private Supplier<SecurityIdentity> build(SecurityIdentity identity, Set<String> roles) {
        // create a new builder and copy principal, attributes, credentials and roles from the original identity
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

        log.tracef("Adding roles=%s", roles);

        // add custom role source here
        builder.addRoles(roles);
        return builder::build;
    }
}
