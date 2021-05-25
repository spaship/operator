package io.spaship.operator.ldap;

import io.quarkus.cache.CacheResult;
import io.quarkus.runtime.StartupEvent;
import io.spaship.operator.rest.website.WebsiteResource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

@ApplicationScoped
public class LdapService {

    private static final Logger log = Logger.getLogger(LdapService.class);

    @ConfigProperty(name = "app.ldap.enabled")
    boolean enabled;
    @ConfigProperty(name = "app.ldap.ctxFactory")
    String ldapCtxFactory;
    @ConfigProperty(name = "app.ldap.url")
    Optional<String> ldapUrl;
    @ConfigProperty(name = "app.ldap.search.name")
    Optional<String> searchName;
    @ConfigProperty(name = "app.ldap.search.filter")
    String searchFilter;
    @ConfigProperty(name = "app.ldap.search.groups.attrName")
    Optional<String> searchGroupAttrName;
    @ConfigProperty(name = "app.ldap.search.role.user.attrValue")
    Optional<String> searchRoleUserAttrValue;
    @ConfigProperty(name = "app.ldap.search.role.admin.attrValue")
    Optional<String> searchRoleAdminAttrValue;

    @ConfigProperty(name = "app.ldap.adminLogin.username")
    Optional<String> adminLoginUsername;
    @ConfigProperty(name = "app.ldap.adminLogin.password")
    Optional<String> adminLoginPassword;

    Properties properties;
    SearchControls controls;

    void onStart(@Observes StartupEvent ev) throws NamingException {
        log.infof("LDAP Service Init. enabled=%s ldapUrl=%s searchName=%s searchFilter=%s adminLoginUsername=%s searchGroupAttrName=%s searchRoleUserAttrValue=%s searchRoleAdminAttrValue=%s",
                enabled, ldapUrl.orElse("N/A"), searchName.orElse("N/A"), searchFilter, adminLoginUsername.orElse("N/A"),
                searchGroupAttrName.orElse("N/A"), searchRoleUserAttrValue.orElse("N/A"), searchRoleAdminAttrValue.orElse("N/A")
        );

        if (!enabled) {
            return;
        }
        if (ldapUrl.isEmpty() || searchName.isEmpty() || searchGroupAttrName.isEmpty() || searchRoleUserAttrValue.isEmpty() || searchRoleAdminAttrValue.isEmpty()) {
            throw new LdapException("Configuration missing. " +
                    "Properties 'app.ldap.url', 'app.ldap.search.name', 'app.ldap.search.groups.attrName', 'app.ldap.search.role.user.attrValue', 'app.ldap.search.role.admin.attrValue'" +
                    " are required");
        }
        initLdap();
    }

    protected void initLdap() throws NamingException {
        // Initialize properties
        // https://docs.oracle.com/javase/jndi/tutorial/ldap/connect/pool.html
        properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, ldapCtxFactory);
        properties.put(Context.PROVIDER_URL, ldapUrl.get());
        properties.put("com.sun.jndi.ldap.connect.pool", "true");
        adminLoginUsername.ifPresent(s -> properties.put(Context.SECURITY_PRINCIPAL, s));
        adminLoginPassword.ifPresent(s -> properties.put(Context.SECURITY_CREDENTIALS, s));

        controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(new String[]{searchGroupAttrName.get()});
    }

    public boolean isEnabled() {
        return enabled;
    }

    @CacheResult(cacheName = "ldap-roles")
    public Set<String> getRoles(String filterValue) {
        log.infof("LDAP Search Roles. filterValue=%s", filterValue);
        Set<String> roles = new HashSet<>();
        try {
            // Initialize ldap context
            DirContext ldapContext = new InitialDirContext(properties);

            String filter = String.format(searchFilter, filterValue);
            NamingEnumeration<SearchResult> results = ldapContext.search(searchName.get(), filter, controls);
            while (results != null && results.hasMore()) {
                SearchResult searchResult = results.next();
                Attribute groups = searchResult.getAttributes().get(searchGroupAttrName.get());
                log.tracef("groups=%s", groups);
                if (groups != null && groups.contains(searchRoleUserAttrValue.get())) {
                    roles.add(WebsiteResource.ROLE_SPASHIP_USER);
                }
                if (groups != null && groups.contains(searchRoleAdminAttrValue.get())) {
                    roles.add(WebsiteResource.ROLE_SPASHIP_ADMIN);
                }
            }
            ldapContext.close();   // Return connection to pool
        } catch (NamingException e) {
            log.error("Cannot query LDAP", e);
            throw new LdapException(e);
        }
        return roles;
    }

}
