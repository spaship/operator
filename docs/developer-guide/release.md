# Release

1. Perform test first
```shell
mvn clean package
```
2. Maven release - Prompted for desired target version
```shell
mvn clean release:prepare release:perform
```
3. Wait till [Deploy to registries](https://github.com/websitecd/operator/actions?query=workflow%3A%22Deploy+to+Registries%22) completes.
4. Create a [Github release](https://github.com/websitecd/operator/releases) based on the latest tag and document the release.
