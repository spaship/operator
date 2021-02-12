# Website Specification


## Environments

Each environment defines:

1. Name (as object key)
2. Default branch
3. Deployments overrides
4. Skip Components

## Components

Every component needs to define its `kind`.
This allows define website as different components resp. their source.

### Component `git`

Content stored in git repository and defined `branch` or `tag`.

```yaml
components:
  - context: /path
    kind: service                  # Service kind - only route/ingress is created
    spec:
      url: https://github.com/websitecd/websitecd-examples.git     # Git URL of repository. Default is Git URL of website.yaml
      dir: /websites/02-advanced/chrome        # Subdirectory within. Default is "."
      branch: "1.0.0"              # Branch/Tag override on component level
      envs:
        dev: "2.0.0"               # Branch/Tag override on environment level
```

### Component `service`

Component has no content and Operator just creates a route for it.

```yaml
components:
  - context: /path
    kind: service
    spec:
      serviceName: searchapi       # Service name
      targetPort: 8080             # Service port
```

## Defaults and Overrides

The `website.yaml` uses strongly defaults & overrides strategy.
For example branch name is defined on following places:

`Environment` -> `Component` -> `Component's environment`

`envs.<env_name>.branch` -> `components.spec.branch` -> `components.spec.envs.<env_name>`

## Complete Spec Reference
The complete `website.yaml` reference

```yaml
apiVersion: 1                      # Website API reference

labels:                            # Labels
  label-key: label-value           # Key is label key, Value is label value

envs:                              # Website environments
  dev:                             # Name of environment
    branch: main                   # Default branch for all componetns
  prod:
    branch: prod
    skipContexts:                  # Skip Components for given 
      - /search
      - /search/api
    deployment:                    # Deployment Overrides 
      replicas: 2                  # Number of replicas
      httpd:                       # Overrides for httpd
        resources:                 # Standard k8s resources
          requests:
            cpu: 100m
            memory: 150Mi
          limits:
            cpu: 500m
            memory: 250Mi

components:                        # Components
  - context: /theme                # Path under component lives
    kind: git                      # Component kind. Available kinds: "git", "service"
    spec:
      url: https://github.com/websitecd/websitecd-examples.git     # Git URL of repository. Default is Git URL of website.yaml
      dir: /websites/02-advanced/chrome        # Subdirectory within. Default is "."
      branch: "1.0.0"              # Branch override on component level
      envs:
        dev: "2.0.0"               # Branch override on environment level
  - context: /search/api
    kind: service                  # Service kind - only route/ingress is created
    spec:
      serviceName: searchapi       # Service name
      targetPort: 8080             # Service port
  - context: /                     # Home SPA
    kind: git
    spec:
      dir: /websites/02-advanced/home
```