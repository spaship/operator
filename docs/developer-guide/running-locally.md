# Running Website CD Locally

## Minikube

```shell
minikube config set driver hyperkit
minikube start --addons ingress,dashboard --cpus 4 --memory 8192
minikube dashboard
# tunnel not needed if using /etc/hosts bellow
# minikube tunnel
echo "$(minikube ip) minikube.info operator-websitecd.minikube.info simple-dev-websitecd-examples.minikube.info simple-prod-websitecd-examples.minikube.info advanced-dev-websitecd-examples.minikube.info advanced-prod-websitecd-examples.minikube.info" | sudo tee -a /etc/hosts

kubectl create namespace websitecd-examples
```

### Logs

```shell
kubectl -n websitecd logs --selector=websitecd-operator-layer=service --tail 10 -f
```

## Local Development

### Build project

```shell
mvn clean install
```

### Run Operator Locally
Default values for dev mode are stored in [application.properties](../../service/src/main/resources/application.properties)
in section `# DEV`

```shell
cd service
mvn quarkus:dev
```

In few seconds the operator is up and connects to k8s cluster.

To view which context do you use just do
```shell
kubectl config current-context
```
To switch to minikube context do
```shell
kubectl config use-context minikube
```

#### Locally In Docker

Build local docker image (skip this to use the latest from the repository)
```shell
docker build -f src/main/docker/Dockerfile.jvm -t websitecd/operator-jvm .
```

```shell
docker run -i --rm -e APP_OPERATOR_PROVIDER_ENV_ENABLED=true -e APP_OPERATOR_PROVIDER_CRD_ENABLED=false -e WEBSITE_NAMESPACE=websitecd-examples \
   -e WEBSITE_NAME=simple -e WEBSITE_GITURL=https://github.com/websitecd/websitecd-examples.git -e WEBSITE_CONFIG_DIR=websites/02-advanced -e WEBSITE_WEBHOOK_SECRET=TOKENSIMPLE \
   -p 8080:8080 websitecd/operator-jvm
```

## Webhook API Development

Init websites git repos

```shell
rm -rf /tmp/repos; mkdir /tmp/repos
cp config/src/test/resources/gitconfig-test.yaml /tmp/repos/static-content-config.yaml
docker run --rm -e "CONFIG_PATH=/app/data/static-content-config.yaml" -e "TARGET_DIR=/app/data" -e "GIT_SSL_NO_VERIFY=true" -v "/tmp/repos/:/app/data/" quay.io/websitecd/content-git-init
```

Start content-git-api on port `8090`

```shell
docker run --rm -e "APP_DATA_DIR=/app/data" -v "/tmp/repos/:/app/data/" -p 8090:8090 quay.io/websitecd/content-git-api
```

Fire event:

```shell
WEBHOOK_URL=http://localhost:8080/api/webhook
# WEBHOOK_URL=http://operator-websitecd.minikube.info/api/webhook
curl -i -X POST $WEBHOOK_URL  -H "Content-Type: application/json" -H "X-Gitlab-Event: Push Hook" -H "X-Gitlab-Token: TOKENSIMPLE" --data-binary "@src/test/resources/gitlab-push.json" 
```

## Build Docker Image

```shell
mvn clean install
cd service
docker build -f src/main/docker/Dockerfile.jvm -t websitecd/operator-jvm .
```
