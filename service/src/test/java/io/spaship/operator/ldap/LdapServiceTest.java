package io.spaship.operator.ldap;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Set;

@QuarkusTest
class LdapServiceTest {

    @Inject
    LdapService ldapService;

    @Test
    void getRoles() {
        Set<String> test1 = ldapService.getRoles(MockInitialDirContextFactory.LDAP_USERNAME);
        Assertions.assertEquals(2, test1.size());
    }

}