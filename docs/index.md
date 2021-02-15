# Website CD Operator

Website Continues Deployment & Delivery on Kubernetes as easy as Github pages.

## Features

Operator covers common enterprise use cases like:

1. Sharing website blocks/components - header, footer, marketing etc.
2. Website as composition of static content, SPAs and other services (REST API, GraphQL, Drupal etc.)
3. Multiple environments deployment. Resources configuration per environment.
4. Git webhooks integration for continuous Deployment and Delivery
5. Extendable - ability to provide another repository of content in addition to git (e.g. FTP)


## The Simplest Use Case

### Step 1 - Describe your website and push it into your git repo
```yaml
apiVersion: v1

# Environments
envs:
  dev:
    branch: main                       # dev git branch (can be git tag)
  prod:
    branch: prod                       # prod git branch (can be git tag e.g. "1.0.0")
    deployment:
      replicas: 2                      # per environment deployment configuration

# List of Website Components / Blocks
components:
  - context: /theme                    # URL context of website shared component
    kind: git
    spec:
      url: https://github.com/websitecd/websitecd-examples.git
      dir: /websites/01-simple/theme   # sub directory within git repo
  - context: /                         # URL context of main SPA
    kind: git
    spec:
      url: https://github.com/websitecd/websitecd-examples.git
      dir: /websites/01-simple/home
```

### Step 2 - Register your git repo in the operator.
Create simple `simple-site.yaml`:

```yaml
apiVersion: websitecd.io/v1
kind: Website
metadata:
  name: simple
spec:
  gitUrl: https://github.com/websitecd/websitecd-examples.git
  branch: main
  dir: websites/01-simple              # Relative path to your website.yaml
  secretToken: TOKENSIMPLE
```   

Apply it
```shell
kubectl create namespace websitecd-examples
kubectl apply -n websitecd-examples -f simple-site.yaml
```   

That's IT!

Operator creates both `dev` and `prod` environment with main `SPA` and `theme` and is ready
to accept Git webhook events for:
* Continues deployment (changes in environments or components)
* Continues delivery (changes in `theme` and `main SPA`).

More examples: https://github.com/websitecd/websitecd-examples.git

## Supported Runtimes

1. Kubernetes
2. Minikube
3. Openshift 3
4. Openshift 4

## How To Install

Operator:
```shell
kubectl create namespace websitecd
kubectl apply -n websitecd -f https://raw.githubusercontent.com/websitecd/operator/main/manifests/config/k8s.yaml
kubectl apply -n websitecd -f https://raw.githubusercontent.com/websitecd/operator/main/manifests/install.yaml
```

Website:
```shell
kubectl create namespace websitecd-examples
# Advanced:
kubectl apply -n websitecd-examples -f https://raw.githubusercontent.com/websitecd/websitecd-examples/main/websites/02-advanced/deployment-advanced-preprodonly.yaml
# Simple:
kubectl apply -n websitecd-examples -f https://raw.githubusercontent.com/websitecd/websitecd-examples/main/websites/01-simple/deployment-simple-allenvs.yaml
```
