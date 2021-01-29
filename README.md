# Website CD Operator

Website Continues Deployment & Delivery on Kubernetes

## REST API

* /health/live
* /health/ready
* /api/webhook


## How to install

```shell
kubectl create namespace websitecd
kubectl create namespace websitecd-examples
kubectl apply -n websitecd -f deployment/stable/config-example.yaml
kubectl apply -n websitecd -f deployment/stable/websitecd-kubernetes.yaml
```

## Development

See [Development Documentation](docs/DEVELOPMENT.md).
