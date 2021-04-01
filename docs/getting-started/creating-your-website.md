# Creating Your Website

## Create Website Manifest

Example of `website.yaml` file

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
      url: https://github.com/spaship/spaship-examples.git
      dir: /websites/01-simple/theme   # sub directory within git repo
  - context: /                         # URL context of main SPA
    kind: git
    spec:
      url: https://github.com/spaship/spaship-examples.git
      dir: /websites/01-simple/home
```

## Push it into your git repo

```shell
git add website.yaml
git push
```

## Reference

See [Website Specification](../user-guide/website-specification.md) for more details.