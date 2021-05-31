# Operator Installation

Operator needs very few configurations in config map and namespace. Here is simple use case. For more details
see [Operator Manual](../operator-manual/installation.md).

If you don't have Kubernetes cluster install the [minikube](https://minikube.sigs.k8s.io/docs/start/).

## Create separate namespace

```shell
kubectl create namespace spaship-operator
```

## Create Operator's configuration

```shell
kubectl create configmap -n spaship-operator spaship-operator-config \
        --from-literal=APP_OPERATOR_ROUTER_MODE=ingress \
        --from-literal=APP_OPERATOR_WEBSITE_DOMAIN=minikube.info
```

## Install Operator

```shell
kubectl apply -n spaship-operator -f https://raw.githubusercontent.com/spaship/operator/main/manifests/install.yaml
```
