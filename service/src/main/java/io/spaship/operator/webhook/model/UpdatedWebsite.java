package io.spaship.operator.webhook.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description="Info about updated website")
public class UpdatedWebsite {
    public static final String STATUS_IGNORED = "IGNORED";
    public static final String STATUS_UPDATING = "UPDATING";

    String name;
    String namespace;
    @Schema(description = "Website Update Status", enumeration = {STATUS_UPDATING, STATUS_IGNORED})
    String status;

    public UpdatedWebsite(String name, String namespace, String status) {
        this.name = name;
        this.namespace = namespace;
        this.status = status;
    }

    public UpdatedWebsite() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
