# Operator Installation

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
