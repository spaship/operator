package io.spaship.operator.rest.website.model;

public class ComponentDetailResponse extends WebsiteApiResponseBase {

    ComponentDetail data;

    public static ComponentDetailResponse success(ComponentDetail data) {
        ComponentDetailResponse response = new ComponentDetailResponse();
        response.setStatus(STATUS_SUCCESS);
        response.setData(data);
        return response;
    }

    public ComponentDetail getData() {
        return data;
    }

    public void setData(ComponentDetail data) {
        this.data = data;
    }
}

