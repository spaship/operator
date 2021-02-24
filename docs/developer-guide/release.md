# Release

1. Perform test first
```shell
mvn clean package
```
2. Update operator's target version `quay.io/websitecd/operator-jvm:<VERSION>` in [install.yaml](/manifests/install.yaml) manifest.
3. Perform maven release - prompted for target version which creates a tag
```shell
mvn clean release:prepare release:perform
```
4. Wait till [Deploy to registries](https://github.com/websitecd/operator/actions?query=workflow%3A%22Deploy+to+Registries%22) completes.
5. Create a [Github release](https://github.com/websitecd/operator/releases) based on the latest tag and document the release.
