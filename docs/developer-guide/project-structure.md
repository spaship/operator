# Project Structure

## Git Repositories

The [Website CD github organization](https://github.com/websitecd/) provides these repos:

1. [Operator](https://github.com/websitecd/operator) - The main operator repo
2. [Content GIT](https://github.com/websitecd/content-git) - component `git` support
3. [Websites Examples](https://github.com/websitecd/websitecd-examples) - various examples

## Maven

Project consists of three modules

1. config - Operator configuration model classes
2. config-validator - `website.yaml` validator and json schema
2. service - Operator main business logic

### Repository

Maven artefacts are pushed to [jboss.org repository](https://repository.jboss.org/nexus/#nexus-search;quick~io.websitecd).

If other project depends on e.g. operator-config artefact
Add this repository to pom.xml: `https://repository.jboss.org/nexus/content/repositories/DXP/` and dependency:
```xml
<dependency>
  <groupId>io.websitecd.operator</groupId>
  <artifactId>operator-config</artifactId>
  <version>1.1.0</version>
</dependency>
```
