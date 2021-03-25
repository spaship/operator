# Website Performance Testsuite

This testsuite covers performance of target website environment.

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
```

## Prepare Watch Windows

Watch deployments:
```shell
kubectl create namespace spaship-examples
kubectl get -n spaship-examples deployment -w
```

Dashboard:
```shell
minikube dashboard
```

## Simple Website Testsuite (no SSI)

Run jmeter
```shell
$JMETER/bin/jmeter.sh -t tests/performance/simple/perftest-simple.jmx &
```

Deploy Website
```shell
cd tests/performance/simple/
kubectl apply -n spaship-examples -f deployment-simple-dev.yaml
```

### Configuration
* 3 requests per test: main page, css, js
* 100 concurrent requests
* 100 loops


#### 500m CPU, 2 replicas
```shell
kubectl apply -n spaship-examples -f patch-cpu-500m.yaml
kubectl scale -n spaship-examples --replicas=2 deployment simple-content-dev
```

Website is available under [simple-dev-spaship-examples.minikube.info](http://simple-dev-spaship-examples.minikube.info/)

#### 500m CPU, 4 replicas
```shell
kubectl apply -n spaship-examples -f patch-cpu-500m.yaml
kubectl scale -n spaship-examples --replicas=4 deployment simple-content-dev
```

#### 1000m CPU, 2 replicas

```shell
kubectl apply -n spaship-examples -f patch-cpu-1000m.yaml
kubectl scale -n spaship-examples --replicas=2 deployment simple-content-dev
```

#### 1000m CPU, 4 replicas

```shell
kubectl apply -n spaship-examples -f patch-cpu-1000m.yaml
kubectl scale -n spaship-examples --replicas=4 deployment simple-content-dev
```

### Cleanup

```shell
kubectl delete -n spaship-examples websites.spaship.io --all
```

## Advanced Website Testsuite (SSI used)

Run jmeter
```shell
$JMETER/bin/jmeter.sh -t tests/performance/advanced/perftest-advanced.jmx &
```

Deploy Website
```shell
cd tests/performance/advanced/
kubectl apply -n spaship-examples -f deployment-advanced-dev.yaml
```

### Configuration
* 3 requests per test: main page (SSI performed), css, js
* 100 concurrent requests
* 100 loops

#### 500m CPU, 2 replicas

```shell
kubectl apply -n spaship-examples -f patch-cpu-500m.yaml
kubectl scale -n spaship-examples --replicas=2 deployment advanced-content-dev
```

Website is available under [advanced-dev-spaship-examples.minikube.info](http://advanced-dev-spaship-examples.minikube.info/)

#### 500m CPU, 4 replicas
```shell
kubectl apply -n spaship-examples -f patch-cpu-500m.yaml
kubectl scale -n spaship-examples --replicas=4 deployment advanced-content-dev
```

#### 1000m CPU, 2 replicas

```shell
kubectl apply -n spaship-examples -f patch-cpu-1000m.yaml
kubectl scale -n spaship-examples --replicas=2 deployment advanced-content-dev
```

#### 1000m CPU, 4 replicas

```shell
kubectl apply -n spaship-examples -f patch-cpu-1000m.yaml
kubectl scale -n spaship-examples --replicas=4 deployment advanced-content-dev
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

| CPU | Replicas | Simple Web | Advanced Web |
|-----|----------|------------|--------------|
| 500m  | 2 | 2100 | 1360 |
| 500m  | 4 | 4300 | 2780 |
| 1000m | 2 | 3000 | 2700 |
| 1000m | 4 | 3700 | 3300 |

Result numbers are throughput requests/second.
