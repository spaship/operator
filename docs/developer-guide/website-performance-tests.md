# Website Performance

## Prepare cluster / minikube

```shell
minikube delete
minikube config set driver hyperkit
minikube config set memory 8192
minikube config set cpus 4
minikube start --addons ingress,dashboard,metrics-server

echo "$(minikube ip) minikube.info operator-websitecd.minikube.info simple-dev-websitecd-examples.minikube.info simple-prod-websitecd-examples.minikube.info advanced-dev-websitecd-examples.minikube.info advanced-prod-websitecd-examples.minikube.info" | sudo tee -a /etc/hosts
```

## Deploy Operator

```shell
kubectl create namespace websitecd
# Operator configuration
kubectl create configmap -n websitecd websitecd-operator-config \
        --from-literal=APP_OPERATOR_ROUTER_MODE=ingress \
        --from-literal=APP_OPERATOR_WEBSITE_DOMAIN=minikube.info
# Operator
kubectl apply -n websitecd -f manifests/install.yaml
```

## Prepare Watch Windows

Watch deployments:
```shell
kubectl create namespace websitecd-examples
kubectl get -n websitecd-examples deployment -w
```

Dashboard:
```shell
minikube dashboard
```

## Simple Website Testsuite (no SSI)

Testsuite:
```shell
$JMETER/bin/jmeter.sh -t tests/performance/simple/perftest-simple.jmx &
```

### 500m CPU, 2 replicas
```shell
cd tests/performance/simple/
kubectl apply -n websitecd-examples -f deployment-simple-dev.yaml
kubectl apply -n websitecd-examples -f patch-cpu-500m.yaml
kubectl scale -n websitecd-examples --replicas=2 deployment simple-content-dev
```

Website is available under [simple-dev-websitecd-examples.minikube.info](http://simple-dev-websitecd-examples.minikube.info/)

### 500m CPU, 4 replicas
```shell
kubectl apply -n websitecd-examples -f patch-cpu-500m.yaml
kubectl scale -n websitecd-examples --replicas=4 deployment simple-content-dev
```

### 1000m CPU, 2 replicas

```shell
kubectl apply -n websitecd-examples -f patch-cpu-1000m.yaml
kubectl scale -n websitecd-examples --replicas=2 deployment simple-content-dev
```

### 1000m CPU, 4 replicas

```shell
kubectl apply -n websitecd-examples -f patch-cpu-1000m.yaml
kubectl scale -n websitecd-examples --replicas=4 deployment simple-content-dev
```

### Cleanup

```shell
kubectl delete -n websitecd-examples websites.websitecd.io --all
```

## Advanced Website Testsuite (SSI used)

Testsuite:
```shell
$JMETER/bin/jmeter.sh -t tests/performance/advanced/perftest-advanced.jmx &
```

### 500m CPU, 2 replicas

```shell
cd tests/performance/advanced/
kubectl apply -n websitecd-examples -f deployment-advanced-dev.yaml
kubectl apply -n websitecd-examples -f patch-cpu-500m.yaml
kubectl scale -n websitecd-examples --replicas=2 deployment advanced-content-dev
```

Website is available under [advanced-dev-websitecd-examples.minikube.info](http://advanced-dev-websitecd-examples.minikube.info/)

### 500m CPU, 4 replicas
```shell
kubectl apply -n websitecd-examples -f patch-cpu-500m.yaml
kubectl scale -n websitecd-examples --replicas=4 deployment advanced-content-dev
```

### 1000m CPU, 2 replicas

```shell
kubectl apply -n websitecd-examples -f patch-cpu-1000m.yaml
kubectl scale -n websitecd-examples --replicas=2 deployment advanced-content-dev
```

### 1000m CPU, 4 replicas

```shell
kubectl apply -n websitecd-examples -f patch-cpu-1000m.yaml
kubectl scale -n websitecd-examples --replicas=4 deployment advanced-content-dev
```

### Cleanup

```shell
kubectl delete -n websitecd-examples websites.websitecd.io --all
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
| 500m  | 2 | 1400 | 1360 |
| 500m  | 4 | 4300 | 2780 |
| 1000m | 2 | 3000 | 2700 |
| 1000m | 4 | 3700 | 3300 |

Result numbers are throughput requests/second.