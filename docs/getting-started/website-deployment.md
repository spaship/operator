# Website Deployment

## Create Deployment Manifest

Example of `simple-site.yaml`:

```yaml
apiVersion: spaship.io/v1
kind: Website
metadata:
  name: simple
spec:
  gitUrl: https://github.com/spaship/spaship-examples.git
  dir: websites/01-simple              # Relative path to your website.yaml
  secretToken: TOKENSIMPLE
```   

## Deploy Website

```shell
kubectl create namespace spaship-examples
kubectl apply -n spaship-examples -f simple-site.yaml
```   

That's IT!

Operator creates both `dev` and `prod` environment with main `SPA` and `theme` and is ready
to accept Git webhook events for:

* Continues deployment (changes in environments or components)
* Continues delivery (changes in `theme` and `main SPA`).

## More examples

See git repository [spaship-examples](https://github.com/spaship/spaship-examples.git).
