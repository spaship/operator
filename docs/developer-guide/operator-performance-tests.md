# Operator Performance Testsuite

This testsuite covers performance of the operator.

## Prepare cluster / minikube

```shell
minikube delete
minikube config set driver hyperkit
minikube config set memory 8192
minikube config set cpus 4
minikube start --addons ingress,dashboard,metrics-server

echo "$(minikube ip) minikube.info operator-spaship.minikube.info simple-dev-spaship-examples.minikube.info simple-prod-spaship-examples.minikube.info advanced-dev-spaship-examples.minikube.info advanced-prod-spaship-examples.minikube.info" | sudo tee -a /etc/hosts
```

## Deploy Operator

```shell
kubectl create namespace spaship
# Operator configuration
kubectl create configmap -n spaship spaship-operator-config \
        --from-literal=APP_OPERATOR_ROUTER_MODE=ingress \
        --from-literal=APP_OPERATOR_WEBSITE_DOMAIN=minikube.info
# Operator
kubectl apply -n spaship -f manifests/install.yaml
kubectl apply -n spaship -f manifests/minikube/ingress.yaml
```

Operator is available under [https://operator-spaship.minikube.info/](https://operator-spaship.minikube.info/).

## Deploy sample website

Deploy Website

```shell
kubectl create namespace spaship-examples
kubectl apply -n spaship-examples -f tests/performance/simple/deployment-simple-dev.yaml
```

And watch:

```shell
kubectl get -n spaship-examples websites.spaship.io -w
```

## Prepare Watch Windows

Watch operator deployments:

```shell
kubectl get -n spaship deployment -w
```

Dashboard:

```shell
minikube dashboard
```

## Webhook 1 - `no matched website or components`

Run jmeter

```shell
cd tests/performance/operator
$JMETER/bin/jmeter.sh -t perftest-operator-webhook.jmx &
```

### Configuration

* 1 requests URL: webhook - `no matched website or components`
* 10 concurrent requests
* 10000 loops

#### 500m CPU, 2 replicas

```shell
kubectl apply -n spaship -f patch-cpu-500m.yaml
kubectl scale -n spaship --replicas=2 deployment spaship-operator
```

Website is available
under [simple-dev-spaship-examples.minikube.info](http://simple-dev-spaship-examples.minikube.info/)

#### 500m CPU, 4 replicas

```shell
kubectl apply -n spaship-examples -f patch-cpu-500m.yaml
kubectl scale -n spaship --replicas=4 deployment spaship-operator
```

#### 1000m CPU, 2 replicas

```shell
kubectl apply -n spaship -f patch-cpu-1000m.yaml
kubectl scale -n spaship --replicas=2 deployment spaship-operator
```

### Cleanup

```shell
kubectl delete -n spaship-examples websites.spaship.io --all
```

## Results

### 2021-03-04

```
Model Identifier:	MacBookPro16,1
Processor Name:	6-Core Intel Core i7
Processor Speed:	2,6 GHz
Number of Processors:	1
Total Number of Cores:	6
L2 Cache (per Core):	256 KB
L3 Cache:	12 MB
Hyper-Threading Technology:	Enabled
Memory:	32 GB
```

| CPU | Replicas | Webhook 1 |
|-----|----------|-----------|
| 500m  | 2 | 527 |
| 500m  | 4 | 800 |
| 1000m | 2 | 1200 |

Result numbers are throughput requests/second.
