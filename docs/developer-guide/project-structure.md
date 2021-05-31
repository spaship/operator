# Project Structure

## Git Repositories

The [SPAship github organization](https://github.com/spaship/) provides these repos:

1. [Operator](https://github.com/spaship/operator) - The main operator repo
2. [Content GIT](https://github.com/spaship/content-git) - component `git` support
3. [SPAship Examples](https://github.com/spaship/spaship-examples) - various examples

## Maven

Project consists of three modules

1. config - Operator configuration model classes
2. config-validator - `website.yaml` validator and json schema
2. service - Operator main business logic

### Repository

Maven artefacts are pushed to [jboss.org repository](https://repository.jboss.org/nexus/#nexus-search;quick~io.spaship).

If other project depends on e.g. operator-config artefact Add this repository to
pom.xml: `https://repository.jboss.org/nexus/content/repositories/DXP/` and dependency:

```xml
<dependency>
  <groupId>io.spaship.operator</groupId>
  <artifactId>operator-config</artifactId>
  <version>1.2.0</version>
</dependency>
```
