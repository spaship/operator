package io.spaship.operator.rest.website.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class WebsiteApiResponseBase {

    public static final String STATUS_SUCCESS = "success";

    @Schema(description = "Response Status", enumeration = {STATUS_SUCCESS})
    String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
