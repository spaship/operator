# Website Management

## Get Website Info

```shell
kubectl get websites.websitecd.io -n websitecd-examples
```
Output is:
```shell
NAME     GIT URL                                               BRANCH   DIR                    SSL VERIFY
simple   https://github.com/websitecd/websitecd-examples.git   main     websites/02-advanced   true
```

## Register Website in Cluster

### Step 1: Create a namespace
```shell
kubectl create namespace website
```

### Step 2: Create a `my-website.yaml` file and apply it in the cluster

```yaml
apiVersion: websitecd.io/v1
kind: Website
metadata:
  name: my-website               # Name of the website. Must be unique within namespace
spec:
  gitUrl: https://github.com/websitecd/websitecd-examples.git
  branch: main                   # Branch
  dir: websites/02-advanced      # Location of website.yaml file. Default is "."
  sslVerify: false               # Perform SSL verification. Default is "true"
  secretToken: TOKENADVANCED     # Secret token for Webhook API
  envs:                          # Control which environment will be deployed to. If not defined all envs are deployed.
```

```shell
kubectl apply -n website -f my-website.yaml
```

### Optional - Control Mapping Environments to Namespaces

Thanks to `exclusion` and `inclusion` an administrator has under full control which environment will be deployed in which namespace.

#### Example - excluded prod environment
```yaml
spec:
  envs:
    excluded:
      - prod
```

#### Example - included only prod environment
```yaml
spec:
  envs:
    included:
      - prod
```

#### Example - included only `pr-*` environment
```yaml
spec:
  envs:
    included:
      - pr-.*
```


## Delete Website in Cluster

Delete website:

```shell
kubectl delete websites.websitecd.io -n website my-website
```

