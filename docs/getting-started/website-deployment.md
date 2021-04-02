# Website Deployment

To deploy your website just create simple deployment manifest which points to your git repository
containing `website.yaml` file. Choose wisely `name` of website - it's used for creating your runtime and configuration.

## Create Deployment Manifest

Example of `my-website.yaml`:

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
kubectl apply -n spaship-examples -f my-website.yaml
```   

That's IT!

Operator creates both `dev` and `prod` environment with main `SPA` and `theme`.

## Continuous Deployment & Delivery

The operator is also ready to consume Git webhook events for:

* Continues Deployment (changes in environments or components)
* Continues Delivery (changes in `theme` and `main SPA`).

See [user guide](../user-guide/website-management.md#register-git-webhook) how to set up webhook.

## More examples

See git repository [spaship-examples](https://github.com/spaship/spaship-examples.git).
