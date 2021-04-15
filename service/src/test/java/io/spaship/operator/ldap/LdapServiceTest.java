package io.spaship.operator.ldap;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Set;

import static io.spaship.operator.ldap.MockInitialDirContextFactory.LDAP_ADMIN_ONLY;
import static io.spaship.operator.ldap.MockInitialDirContextFactory.LDAP_USER_ONLY;
import static io.spaship.operator.rest.website.WebsiteResource.ROLE_SPASHIP_ADMIN;
import static io.spaship.operator.rest.website.WebsiteResource.ROLE_SPASHIP_USER;

@QuarkusTest
class LdapServiceTest {

    @Inject
    LdapService ldapService;

    @Test
    void getRoles() {
        Set<String> roles = ldapService.getRoles(MockInitialDirContextFactory.LDAP_ALL_ROLES);
        Assertions.assertEquals(2, roles.size());
    }

    @Test
    void noRoles() {
        Set<String> roles = ldapService.getRoles("user-no-roles");
        Assertions.assertEquals(0, roles.size());
    }

    @Test
    void userRole() {
        Set<String> roles = ldapService.getRoles(LDAP_USER_ONLY);
        Assertions.assertEquals(1, roles.size());
        Assertions.assertEquals(ROLE_SPASHIP_USER, roles.iterator().next());
    }

    @Test
    void adminRole() {
        Set<String> roles = ldapService.getRoles(LDAP_ADMIN_ONLY);
        Assertions.assertEquals(1, roles.size());
        Assertions.assertEquals(ROLE_SPASHIP_ADMIN, roles.iterator().next());
    }

}