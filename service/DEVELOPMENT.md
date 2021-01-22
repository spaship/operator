# Development

## Webhook API

Start content-git-api

```shell
APP_DATA_DIR=/tmp/repos java -jar target/content-git-api-1.0.0-SNAPSHOT-runner.jar
```

Fire event:

```shell
curl -i -X POST http://localhost:8080/api/webhook  -H "Content-Type: application/json" -H "X-Gitlab-Event: Push Hook" --data-binary "@src/test/resources/gitlab-push.json" 
```
