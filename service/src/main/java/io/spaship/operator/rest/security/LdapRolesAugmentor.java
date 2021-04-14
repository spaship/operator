package io.spaship.operator.rest.security;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.spaship.operator.ldap.LdapService;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous() || !ldapService.isEnabled()) {
            return sameIdentity(identity);
        }

        Set<String> roles = ldapService.getRoles(identity.getPrincipal().getName());
        if (roles.isEmpty()) {
            return sameIdentity(identity);
        }

        return Uni.createFrom().item(build(identity, roles));
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
