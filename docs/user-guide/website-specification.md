# Website Specification

Website is specified in `website.yaml` and covers [Environments](#environments) and [Components](#components).

## Environments

Each environment defines:

1. Name (as object key)
2. Default branch
3. Deployments overrides
4. Skip Components

Example:

```yaml
envs:
  dev:
    branch: main
  prod:
    branch: prod
```

## Components

Every component needs to define its `kind`.
This allows design a website as different components with their sources.

### Component `git`

Content stored in git repository in particular `branch` or `tag`.

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

The `website.yaml` strongly uses defaults & overrides strategy.
For example branch name is defined on following places:

`Environment` -> `Component` -> `Component's environment`

`envs.<env_name>.branch` -> `components.spec.branch` -> `components.spec.envs.<env_name>`

This offers flexibility to design each environment differently.

## Complete Spec Reference
The complete `website.yaml` reference

```yaml
apiVersion: v1                     # Website API reference

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