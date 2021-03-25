# Running SPAship Operator Locally

## Minikube

```shell
minikube config set driver hyperkit
minikube start --addons ingress,dashboard --cpus 4 --memory 8192
minikube dashboard
# tunnel not needed if using /etc/hosts bellow
# minikube tunnel
echo "$(minikube ip) minikube.info operator-spaship.minikube.info simple-dev-spaship-examples.minikube.info simple-prod-spaship-examples.minikube.info advanced-dev-spaship-examples.minikube.info advanced-prod-spaship-examples.minikube.info" | sudo tee -a /etc/hosts

kubectl create namespace spaship-examples
```

### Logs

```shell
kubectl -n spaship logs --selector=spaship-operator-layer=service --tail 10 -f
```

## Local Development

The only CRD needs to be registered:
```shell
kubectl apply -f manifests/minikube/crd.yaml
```

### Build project

```shell
mvn clean install
```

### Dev Mode
Default values for dev mode are stored in [application.properties](../../service/src/main/resources/application.properties)
in section `# DEV`

```shell
cd service
mvn quarkus:dev
```

In few seconds the operator is up and connects to k8s cluster and listening to CRD changes.

Register website [simple](https://github.com/spaship/spaship-examples/tree/main/websites/01-simple) or [advanced](https://github.com/spaship/spaship-examples/tree/main/websites/02-advanced).

#### Setting kubectl context
To view which context do you use just do:
```shell
kubectl config current-context
```
To switch to minikube context do:
```shell
kubectl config use-context minikube
```


## Webhook API Development

Init websites git repos

```shell
rm -rf /tmp/repos; mkdir /tmp/repos
cp config/src/test/resources/gitconfig-test.yaml /tmp/repos/static-content-config.yaml
docker run --rm -e "CONFIG_PATH=/app/data/static-content-config.yaml" -e "TARGET_DIR=/app/data" -e "GIT_SSL_NO_VERIFY=true" -v "/tmp/repos/:/app/data/" quay.io/spaship/content-git-init
```

Start content-git-api on port `8090`

```shell
docker run --rm -e "APP_DATA_DIR=/app/data" -v "/tmp/repos/:/app/data/" -p 8090:8090 quay.io/spaship/content-git-api
```

Fire event:

```shell
WEBHOOK_URL=http://localhost:8080/api/webhook
# WEBHOOK_URL=http://operator-spaship.minikube.info/api/webhook
curl -i -X POST $WEBHOOK_URL  -H "Content-Type: application/json" -H "X-Gitlab-Event: Push Hook" -H "X-Gitlab-Token: TOKENSIMPLE" --data-binary "@src/test/resources/gitlab-push.json" 
```

## Build Docker Image

You don't need to care about building docker images because they're covered by [Github Action](https://github.com/spaship/content-git/actions/workflows/docker-publish.yaml)
but for development purposes you can do it locally:

```shell
mvn clean install
cd service
docker build -f src/main/docker/Dockerfile.jvm -t spaship/operator-jvm .
```

Run image:
```shell
docker run -i --rm -e APP_OPERATOR_PROVIDER_ENV_ENABLED=true -e APP_OPERATOR_PROVIDER_CRD_ENABLED=false -e WEBSITE_NAMESPACE=spaship-examples \
   -e WEBSITE_NAME=simple -e WEBSITE_GITURL=https://github.com/spaship/spaship-examples.git -e WEBSITE_CONFIG_DIR=websites/02-advanced -e WEBSITE_WEBHOOK_SECRET=TOKENSIMPLE \
   -p 8080:8080 spaship/operator-jvm
```
