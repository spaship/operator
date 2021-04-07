package io.spaship.operator.content;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description="Response from Component response call")
public class UpdatedComponent {
    @Schema(description="Component name")
    String name;
    @Schema(description="Git update result")
    String status;
    String namespace;
    @Schema(description="Website name")
    String website;
    @Schema(description="Website environment")
    String env;

    public UpdatedComponent(String name, String status, String namespace, String website, String env) {
        this.name = name;
        this.status = status;
        this.namespace = namespace;
        this.website = website;
        this.env = env;
    }

    public UpdatedComponent() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }
}
