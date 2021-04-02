# Installation

It's advised to install operator in separate namespace.

```shell
kubectl create namespace spaship-operator
```

## Configuration

Configuration is managed via ConfigMap which is mapped as environment variables.
List of important configuration properties is listed in [Operator Configuration](configuration.md) page.

### Kubernetes Configuration

```shell
kubectl create configmap -n spaship-operator spaship-operator-config \
        --from-literal=APP_OPERATOR_ROUTER_MODE=ingress \
        --from-literal=APP_OPERATOR_WEBSITE_DOMAIN=minikube.info
```

### Openshift Configuration

```shell
kubectl create configmap -n spaship-operator spaship-operator-config \
        --from-literal=APP_OPERATOR_ROUTER_MODE=openshift \
        --from-literal=APP_OPERATOR_WEBSITE_DOMAIN=minikube.info
```

## Operator Installation

You can manually install all necessary assets of operator by:

```shell
kubectl apply -n spaship-operator -f https://raw.githubusercontent.com/spaship/operator/main/manifests/install.yaml
```

### Operator's Ingress/Route

Operator doesn't create automatically Ingress/Route so they need to be created manually:

[Example](https://raw.githubusercontent.com/spaship/operator/main/manifests/minikube/ingress.yaml) of ingress with host [operator-spaship.minikube.info](http://operator-spaship.minikube.info).
```shell
kubectl apply -n spaship-operator -f https://raw.githubusercontent.com/spaship/operator/main/manifests/minikube/ingress.yaml
```

Example of Openshift route:
```shell
oc create route -n spaship-operator edge operator --service=spaship-operator --hostname=spaship-operator.example.com
```
