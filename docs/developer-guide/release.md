# Release

Perform test first

```shell
mvn clean package
```

1. Maven release
```shell
mvn clean release:prepare release:perform
```
2. Wait till [Deploy to registries](https://github.com/websitecd/operator/actions?query=workflow%3A%22Deploy+to+Registries%22) completes.
3. Create a [Github release](https://github.com/websitecd/operator/releases) based on the latest tag and document the release.
