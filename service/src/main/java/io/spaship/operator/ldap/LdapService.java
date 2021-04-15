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
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
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

    LdapContext ldapContext;
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
        Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, ldapCtxFactory);
        properties.put(Context.PROVIDER_URL, ldapUrl.get());
        adminLoginUsername.ifPresent(s -> properties.put(Context.SECURITY_PRINCIPAL, s));
        adminLoginPassword.ifPresent(s -> properties.put(Context.SECURITY_CREDENTIALS, s));

        // Initialize ldap context
        ldapContext = new InitialLdapContext(properties, null);

        controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(new String[]{searchGroupAttrName.get()});
    }

    public boolean isEnabled() {
        return enabled;
    }

    @CacheResult(cacheName = "ldap-roles")
    public Set<String> getRoles(String username) {
        log.infof("LDAP Search Roles. username=%s", username);
        Set<String> result = new HashSet<>();
        try {
            String filter = String.format(searchFilter, username);
            NamingEnumeration<SearchResult> results = ldapContext.search(searchName.get(), filter, controls);
            if (results == null) {
                return result;
            }
            while (results.hasMore()) {
                SearchResult searchResult = results.next();
                Attributes attributes = searchResult.getAttributes();
                Attribute groups = attributes.get(searchGroupAttrName.get());
                log.tracef("groups=%s", groups);
                if (groups.contains(searchRoleUserAttrValue.get())) {
                    result.add(WebsiteResource.ROLE_SPASHIP_USER);
                }
                if (groups.contains(searchRoleAdminAttrValue.get())) {
                    result.add(WebsiteResource.ROLE_SPASHIP_ADMIN);
                }
            }
            return result;
        } catch (NamingException e) {
            log.error("Cannot query LDAP", e);
            throw new LdapException(e);
        }
    }

}
