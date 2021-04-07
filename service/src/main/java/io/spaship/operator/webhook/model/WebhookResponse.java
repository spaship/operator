package io.spaship.operator.webhook.model;

import io.spaship.operator.content.UpdatedComponent;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description="Webhook Response - status and list of updated components and websites")
public class WebhookResponse {
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PING = "PING";

    @Schema(description = "Response Status", enumeration = {STATUS_SUCCESS, STATUS_PING})
    String status;

    @Schema(description = "List of updated components")
    List<UpdatedComponent> components;

    @Schema(description = "List of updated/ignored websites")
    List<UpdatedWebsite> websites;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<UpdatedComponent> getComponents() {
        return components;
    }

    public void setComponents(List<UpdatedComponent> components) {
        this.components = components;
    }

    public List<UpdatedWebsite> getWebsites() {
        return websites;
    }

    public void setWebsites(List<UpdatedWebsite> updatedWebsites) {
        this.websites = updatedWebsites;
    }
}
