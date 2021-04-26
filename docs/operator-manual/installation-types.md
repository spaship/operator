# Installation Types

## Multi Tenant Configuration

Default installation is that operator is running within own namespace and controls websites within
their namespaces defined by CRDs.

## One Tenant Configuration

It's possible to completely disable Custom Resource Definition and define only one website that will be managed.

Operator needs to have defined these properties:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: spaship-operator-config
data:
  APP_OPERATOR_ROUTER_MODE: 'ingress'
  APP_OPERATOR_WEBSITE_DOMAIN: 'minikube.info'
  APP_OPERATOR_PROVIDER_ENV_ENABLED: 'true'
  APP_OPERATOR_PROVIDER_CRD_ENABLED: 'false'
  WEBSITE_NAMESPACE: 'spaship-examples'
  WEBSITE_NAME: 'simple'
  WEBSITE_GITURL: 'https://github.com/spaship/spaship-examples.git'
  WEBSITE_SSLVERIFY: 'true'                             # Optional
  WEBSITE_BRANCH: 'main'                                # Optional
  WEBSITE_CONFIG_DIR: 'websites/02-advanced'            # Optional
  WEBSITE_WEBHOOK_SECRET: 'CHANGEIT'
  WEBSITE_PREVIEWS: 'true'
```

Then Operator manage the website within same namespace as is installed.

## Namespaced Installation

For limited installation without ClusterRole permissions it's possible to install the Operator within the namespace
with `Role` instead of `ClusterRole`. It's up to configuration if Multi or One tenant way of management is used. Both are supported.
