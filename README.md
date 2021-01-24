# Website CD Operator

Website Continues Deployment & Delivery on Kubernetes

## REST API

* /health/live
* /health/ready
* /api/webhook


## Installation


```shell
kubectl apply -f deployment/config.yaml
kubectl apply -f deployment/service-account.yaml
kubectl apply -f deployment/operator.yaml
```
