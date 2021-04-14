package io.spaship.operator.ldap;

import org.mockito.Mockito;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.spi.InitialContextFactory;
import java.util.Hashtable;

public class MockInitialDirContextFactory implements InitialContextFactory {

    public static final String LDAP_USERNAME = "ldapuser";

    private static DirContext mockContext = null;

    public Context getInitialContext(Hashtable environment) throws NamingException {
        synchronized (MockInitialDirContextFactory.class) {
            mockContext = Mockito.mock(DirContext.class);
            Mockito.when(mockContext.search(
                    Mockito.eq("ou=users,dc=test,dc=com"), Mockito.eq("uid=" + LDAP_USERNAME), Mockito.any(SearchControls.class)))
                    // a custom 'answer', which records the queries issued
                    .thenAnswer(invocationOnMock -> {
                        Attributes attributes = new BasicAttributes();
                        BasicAttribute values = new BasicAttribute("groups");
                        values.add("cn=spaship-users,ou=adhoc,ou=managedGroups,dc=test,dc=com");
                        values.add("cn=spaship-admins,ou=adhoc,ou=managedGroups,dc=test,dc=com");
                        attributes.put(values);

                        return new MockNamingEnumeration(attributes);
                    });
        }
        return mockContext;
    }


    public class MockNamingEnumeration implements NamingEnumeration<SearchResult> {

        SearchResult result;

        public MockNamingEnumeration(Attributes attributes) {
            result = new SearchResult("", null, attributes);
        }

        int index = 0;

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