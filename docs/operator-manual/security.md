# Security

Exposed APIs are secured in different ways based on use case.

## Webhook Security

The `api/webhook` is secured by `securityToken` defined in deployment descriptor.

Any webhook request requires security token (based on git provider), and the operator match all websites secured by
given security token and perform the appropriate action.

In case that one git repository is used for more websites (e.g. common components like header/footer), and they use same
branch then each website needs to register its own webhook.

## API Security

Operator's API is secured primarily via JWT Token and is able to perform authentication and also map JWT groups to the
roles.

### Role Based Authorization

Operator Roles:

* `spaship-user` - read only actions
* `spaship-admin` - admin actions

### JWT Token

To enable JWT just define auth server url and client id by env variables:

* `QUARKUS_OIDC_AUTH_SERVER_URL` - auth server url
* `QUARKUS_OIDC_CLIENT_ID` - client id

Ideally auth server is configured to propagate appropriate groups within access tokens. In case that group names are
different see docs how
to [configure the mapping](https://quarkus.io/guides/security-openid-connect#token-claims-and-securityidentity-roles).

See [complete reference docs](https://quarkus.io/guides/security-openid-connect#configuring-using-the-application-properties-file)

### CORS

Cors is enabled by default with `*` allowing any domain access the API. To configure more precisely
see [CORS docs](https://quarkus.io/guides/http-reference#cors-filter).

### LDAP Groups

Operator provides LDAP groups to roles mapping as alternative way to JWT Token Groups. It's possible to use both
techniques where JWT provides username and its roles and LDAP queries for additional roles.

To enable LDAP define following variables:

* `APP_LDAP_ENABLED` - Enable it by `true`. By default `false`
* `APP_LDAP_URL` - Ldap URL e.g. `ldap://ldap.corp.redhat.com`
* `APP_LDAP_SEARCH_NAME` - LDAP Query Search
* `APP_LDAP_SEARCH_FILTER` - Filter format. Default is `uid=%s` where %s is replaced by JWT username or by claim value
  if `APP_LDAP_JWT_CLAIM` is defined.
* `APP_LDAP_SEARCH_GROUPS_ATTRNAME` - LDAP attribute containing groups
* `APP_LDAP_SEARCH_ROLE_USER_ATTRVALUE` - group name for `user` role
* `APP_LDAP_SEARCH_ROLE_ADMIN_ATTRVALUE` - group name for `admin` role
* `APP_LDAP_JWT_CLAIM` - if set the filter value is used from claim name.
