# Website Management

## Get Website Info

```shell
kubectl get websites.spaship.io -n spaship-examples
```
Output is:
```shell
NAME       GIT URL                                               BRANCH   DIR                    SSL VERIFY   ENVIRONMENTS               STATUS     MESSAGE
advanced   https://github.com/spaship/spaship-examples.git                websites/02-advanced   true         ["dev[1/1]"]               Deployed
simple     https://github.com/spaship/spaship-examples.git                websites/01-simple     true         ["prod[1/1]","dev[1/1]"]   Deployed
```

* STATUS - Status of website. Can be: `Git Clonning`, `Git Pulling`, `Creating`, `Deployed`, `Failed`
* ENVIRONMENTS - Actual deployed environments and their ready/desired replicas.
* MESSAGE - Used for `Failed` status

## Register Website in Cluster

### Step 1: Create a namespace
```shell
kubectl create namespace spaship-examples
```

### Step 2: Create a `my-website.yaml` file and apply it in the cluster

```yaml
apiVersion: spaship.io/v1
kind: Website
metadata:
  name: my-website               # Name of the website. Must be unique within namespace
spec:
  gitUrl: https://github.com/spaship/spaship-examples.git
  branch: main                   # Branch (Optional)
  dir: websites/01-simple        # Location of website.yaml file. Default is "."
  sslVerify: false               # Perform SSL verification. Default is "true"
  secretToken: TOKENADVANCED     # Secret token for Webhook API
  envs:                          # Control which environment will be deployed to. If not defined all envs are deployed.
```

```shell
kubectl apply -n spaship-examples -f my-website.yaml
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

#### Example - included only `pr-*` environments
```yaml
spec:
  envs:
    included:
      - pr-.*
```

## Delete Website in Cluster

Delete website:

```shell
kubectl delete websites.spaship.io -n website my-website
```

Delete namespace:

```shell
kubectl delete namespace website
```
