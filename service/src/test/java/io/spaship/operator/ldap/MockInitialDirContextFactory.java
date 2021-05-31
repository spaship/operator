package io.spaship.operator.ldap;

import org.mockito.Mockito;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.spi.InitialContextFactory;
import java.util.Hashtable;

public class MockInitialDirContextFactory implements InitialContextFactory {

    public static final String LDAP_ALL_ROLES = "ldapuser-allroles";
    public static final String LDAP_USER_ONLY = "ldapuser-user";
    public static final String LDAP_ADMIN_ONLY = "ldapuser-admin";
    private static DirContext mockContext = null;
    final String groupUser = "cn=spaship-users,ou=adhoc,ou=managedGroups,dc=test,dc=com";
    final String groupAdmin = "cn=spaship-admins,ou=adhoc,ou=managedGroups,dc=test,dc=com";
    final String search = "ou=users,dc=test,dc=com";

    public Context getInitialContext(Hashtable environment) throws NamingException {
        synchronized (MockInitialDirContextFactory.class) {
            mockContext = Mockito.mock(DirContext.class);
            Mockito.when(mockContext.search(Mockito.eq(search), Mockito.eq("testid=" + LDAP_ALL_ROLES), Mockito.any(SearchControls.class)))
                    .thenAnswer(invocationOnMock -> {
                        Attributes attributes = new BasicAttributes();
                        BasicAttribute values = new BasicAttribute("groups");
                        values.add(groupUser);
                        values.add(groupAdmin);
                        attributes.put(values);

                        return new MockNamingEnumeration(attributes);
                    });
            Mockito.when(mockContext.search(Mockito.eq(search), Mockito.eq("testid=" + LDAP_USER_ONLY), Mockito.any(SearchControls.class)))
                    .thenAnswer(invocationOnMock -> new MockNamingEnumeration(new BasicAttributes("groups", groupUser)));
            Mockito.when(mockContext.search(Mockito.eq(search), Mockito.eq("testid=" + LDAP_ADMIN_ONLY), Mockito.any(SearchControls.class)))
                    .thenAnswer(invocationOnMock -> new MockNamingEnumeration(new BasicAttributes("groups", groupAdmin)));
        }
        return mockContext;
    }


    public class MockNamingEnumeration implements NamingEnumeration<SearchResult> {

        SearchResult result;
        int index = 0;

        public MockNamingEnumeration(Attributes attributes) {
            result = new SearchResult("", null, attributes);
        }

        @Override
        public SearchResult next() {
            index++;
            return result;
        }

        @Override
        public boolean hasMore() {
            return index < result.getAttributes().size();
        }

        @Override
        public void close() {
            index = 0;
        }

        @Override
        public boolean hasMoreElements() {
            return hasMore();
        }

        @Override
        public SearchResult nextElement() {
            return next();
        }
    }
}