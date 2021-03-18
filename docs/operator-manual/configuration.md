# Operator Configuration

## Important Configuration Properties

| Environment Variable | Default | Description |
| ---------------------|---------|-------------|
| APP_OPERATOR_ROUTER_MODE | disabled | Router module. Values: `ingress` or `openshift` |
| APP_OPERATOR_PROVIDER_CRD_ENABLED | true | If `false` CRD is not watched |
| APP_OPERATOR_PROVIDER_ENV_ENABLED | false | If `true` then operator expects website definition via ENV variable. See [example](https://github.com/websitecd/operator/blob/main/manifests/config/k8s.yaml#L15) |
| APP_OPERATOR_IMAGE_INIT_NAME | quay.io/websitecd/content-git-init | Init image name |
| APP_OPERATOR_IMAGE_INIT_VERSION | 1.1.2 | Init image version |
| APP_OPERATOR_IMAGE_HTTPD_NAME | spaship/httpd | Httpd image name |
| APP_OPERATOR_IMAGE_HTTPD_VERSION | 0.1.0 | Httpd image version |
| APP_OPERATOR_IMAGE_API_NAME | quay.io/websitecd/content-git-api | Api image name |
| APP_OPERATOR_IMAGE_API_VERSION | 1.1.2 | Api image version |

