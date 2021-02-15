# Config Validator

1. The actual schema stored in [src/main/resources/websiteconfig-schema.json](src/main/resources/websiteconfig-schema.json).
2. Validator
3. Generates a schema during build into `target/websiteconfig-schema.json`.

## How to run in JVM mode

via parameter (more files to validate)
```shell
java -jar target/operator-config-validator-1.0.1-SNAPSHOT-runner.jar src/test/resources/valid-simple-website.yaml src/test/resources/valid-advanced-website.yaml
```

via env variable
```shell
APP_FILE_PATH=src/test/resources/valid-simple-website.yaml java -jar target/operator-config-validator-1.0.1-SNAPSHOT-runner.jar
```

## Docker

```shell
docker build -f src/main/docker/Dockerfile.jvm -t websitecd/config-validator-jvm .
```

```shell
cp src/test/resources/*.yaml /tmp
docker run -i --rm -e APP_FILE_PATH=/app/data/valid-simple-website.yaml -v /tmp:/app/data/ websitecd/config-validator-jvm
```

```shell
cp src/test/resources/*.yaml /tmp
docker run -i --rm -v /tmp:/app/data/ websitecd/config-validator-jvm /app/data/valid-simple-website.yaml /app/data/valid-advanced-website.yaml
```
