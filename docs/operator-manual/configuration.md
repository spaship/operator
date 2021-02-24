# Operator Configuration

## Important Configuration Properties

| Environment Variable | Default | Description |
| ---------------------|---------|-------------|
| APP_OPERATOR_ROUTER_MODE | disabled | Router module. Values: `ingress` or `openshift` |
| APP_OPERATOR_PROVIDER_CRD_ENABLED | true | If `false` CRD is not watched |
| APP_OPERATOR_PROVIDER_ENV_ENABLED | false | If `true` then operator expects website definition via ENV variable. See [example](https://github.com/websitecd/operator/blob/main/manifests/config/k8s.yaml#L15) |

