# Operator Configuration

## Configuration Properties

Any Operator's configuration defined in [application.properties](https://github.com/spaship/operator/blob/main/service/src/main/resources/application.properties)
can be overridden via env variable but in uppercase and dot is replaced by underscore.

### Important Configurations

| Environment Variable | Default | Description |
| ---------------------|---------|-------------|
| APP_OPERATOR_ROUTER_MODE | disabled | Router module. Values: `ingress` or `openshift` or `disabled` |
| APP_OPERATOR_PROVIDER_CRD_ENABLED | true | If `false` CRD is not watched |
| APP_OPERATOR_PROVIDER_ENV_ENABLED | false | If `true` then operator expects website definition via ENV variable. See [example](https://github.com/spaship/operator/blob/main/manifests/config/k8s.yaml#L15) |
| APP_OPERATOR_URL | | Operator's URL - used to generate API links in REST API |
| APP_OPERATOR_IMAGE_INIT_NAME | quay.io/spaship/content-git-init | Init image name |
| APP_OPERATOR_IMAGE_INIT_VERSION | 1.3.0 | Init image version |
| APP_OPERATOR_IMAGE_HTTPD_NAME | quay.io/spaship/httpd | Httpd image name |
| APP_OPERATOR_IMAGE_HTTPD_VERSION | 0.1.0 | Httpd image version |
| APP_OPERATOR_IMAGE_API_NAME | quay.io/spaship/content-git-api | Api image name |
| APP_OPERATOR_IMAGE_API_VERSION | 1.3.0 | Api image version |
| APP_OPERATOR_CONTENT_ENVS | | Operator's website environment overrides. Useful for defining default deployment overrides for particular environments e.g. prod |
| QUARKUS_OIDC_AUTH_SERVER_URL |  | Open ID Connect Auth Server for REST API authentication. See [Complete reference](https://quarkus.io/guides/security-openid-connect#configuring-using-the-application-properties-file) |
| QUARKUS_OIDC_CLIENT_ID |  | Open ID Connect Client ID |

## Environment Defaults

It's common to have defined deployment defaults for particular environment. Typically production.

The value of `APP_OPERATOR_CONTENT_ENVS` property can define in JSON format the default values for `env` section for all websites.
Each website but can override this default in its `website.yaml` spec. 

Example:
```json
{
  "envs": {
    "prod": {
      "deployment": {
        "replicas": 2,
        "httpd": {
          "resources": {
            "requests": {
              "cpu": "100m",
              "memory": "150Mi"
            },
            "limits": {
              "cpu": "500m",
              "memory": "250Mi"
            }
          }
        }
      }
    }
  }
}
```

Compressed:
```json
{"envs":{"prod":{"deployment":{"replicas":2,"httpd":{"resources":{"requests":{"cpu":"100m","memory":"150Mi"},"limits":{"cpu":"500m","memory":"250Mi"}}}}}}}
```