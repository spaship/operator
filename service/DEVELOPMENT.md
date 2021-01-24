# Development

## Minikube

```shell
minikube start
minikube dashboard
minikube tunnel

kubectl create namespace catalog-preprod
```

## Local Development

```shell
mvn quarkus:dev
```


## Build Local Docker

```shell
mvn clean package
docker build -f src/main/docker/Dockerfile.jvm -t websitecd/operator-jvm .
docker tag websitecd/operator-jvm quay.io/websitecd/operator-jvm
docker push quay.io/websitecd/operator-jvm

kubectl rollout restart deployment websitecd-operator
```

## Webhook API

Start content-git-api

```shell
APP_DATA_DIR=/tmp/repos java -jar target/content-git-api-1.0.0-SNAPSHOT-runner.jar
```

Fire event:

```shell
WEBHOOK_URL=http://localhost:8080/api/webhook
WEBHOOK_URL=http://websitecd-operator-mwes-swel--websitecd.int.open.paas.redhat.com/api/webhook
curl -i -X POST $WEBHOOK_URL  -H "Content-Type: application/json" -H "X-Gitlab-Event: Push Hook" -H "X-Gitlab-Token: CHANGEIT" --data-binary "@src/test/resources/gitlab-push.json" 
curl -i -X POST $WEBHOOK_URL  -H "Content-Type: application/json" -H "X-Gitlab-Event: Push Hook" -H "X-Gitlab-Token: CHANGEIT" --data-binary "@src/test/resources/gitlab-push-website-changed.json" 
```
