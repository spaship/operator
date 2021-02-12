# Overview

## Maven

### Modules
1. config
2. service

### Repository

Maven artefacts are pushed to [jboss.org repository](https://repository.jboss.org/nexus/#nexus-search;quick~io.websitecd).

If other project depends on e.g. operator-config artefact
Add this repository to pom.xml: `https://repository.jboss.org/nexus/content/repositories/DXP/` and dependency:
```xml
<dependency>
  <groupId>io.websitecd.operator</groupId>
  <artifactId>operator-config</artifactId>
  <version>1.0.0</version>
</dependency>
```

