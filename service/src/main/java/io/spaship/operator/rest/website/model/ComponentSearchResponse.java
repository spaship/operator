package io.spaship.operator.rest.website.model;

import java.util.List;

public class ComponentSearchResponse extends WebsiteApiResponseBase {

    List<Component> data;

    public static ComponentSearchResponse success(List<Component> data) {
        ComponentSearchResponse response = new ComponentSearchResponse();
        response.setStatus(STATUS_SUCCESS);
        response.setData(data);
        return response;
    }

    public List<Component> getData() {
        return data;
    }

    public void setData(List<Component> data) {
        this.data = data;
    }
}

