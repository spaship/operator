# Live Website Development

The operator allows to deploy a website into any cluster including local k8s clusters e.g. minikube.

To enable Live Website Coding experience it's needed to provide:

1. Local files sync to the environment
2. Port forwarding from the cluster to localhost

These two features are nicely provided by [Okteto](https://okteto.com/).

## How to enable Live Website Development

1. Deploy a website via operator like any other environment or just use existing local or remote deployment
2. Install [Okteto CLI](https://okteto.com/docs/getting-started/installation/index.html)
3. Define which files should be synced via `okteto.yaml` configuration and optionally forward ports of other services
   e.g. REST API / DB etc.

### Guideline for `okteto.yaml`

```yaml
name: simple-content-dev          # Name of the website's target deployment managed by operator (Required)
command: httpd -D FOREGROUND      # Same command as spaship/httpd (Required)

# Mapping local directories to website's runtime container (spaship/httpd).
# Can be just few of them or even new components that are not part of website.yaml
# Root directory is `/var/www/html/`
sync:
  - ../02-advanced/chrome:/var/www/html/template/
  - ../shared-components:/var/www/html/shared-components/
  - ../02-advanced/search:/var/www/html/search/
  - ../02-advanced/home:/var/www/html/

persistentVolume:
  enabled: false   # Volumes are disabled because they're initiated by init container

forward:
 - 8080:8080       # Forward the spaship/httpd port to localhost

resources:
  limits:
    cpu: "100m"
    memory: 100Mi

# namespace: spaship-examples   # Optionally hard code the name of namespace
```

## Starting and Stopping Live Coding

Starting live coding experience simple as

```shell
okteto up -n spaship-examples
```

Website is available under [http://localhost:8080](http://localhost:8080) and any changes on local files are
automatically synced to dev environment.

Stopping live coding and rolling back to original state as simple as

```shell
okteto down -n spaship-examples
```

## Live REST API Development

In exactly same way any website service can be developed like REST API.

See
the [SPA + REST API + Mongo Website Example](https://github.com/spaship/spaship-examples/tree/main/websites/03-spa-restapi-mongo)
.

## Examples

All website examples demonstrates how Okteto can be used:

* [Simple Website](https://github.com/spaship/spaship-examples/tree/main/websites/01-simple#local-live-development-by-okteto)
  and [okteto.yaml](https://github.com/spaship/spaship-examples/blob/main/websites/01-simple/okteto.yaml)
* [Advanced Website](https://github.com/spaship/spaship-examples/tree/main/websites/02-advanced#local-live-development-by-okteto)
  and [okteto.yaml](https://github.com/spaship/spaship-examples/blob/main/websites/02-advanced/okteto.yaml)
* [SPA + REST API + Mongo Website](https://github.com/spaship/spaship-examples/tree/main/websites/03-spa-restapi-mongo#local-live-development-by-okteto)
  and [okteto.yaml](https://github.com/spaship/spaship-examples/blob/main/websites/03-spa-restapi-mongo/okteto.yaml)
