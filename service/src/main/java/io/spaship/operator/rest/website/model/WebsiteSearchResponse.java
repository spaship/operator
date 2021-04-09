package io.spaship.operator.rest.website.model;

import java.util.List;

public class WebsiteSearchResponse extends WebsiteApiResponseBase {

    List<WebsiteResponse> data;

    public static WebsiteSearchResponse success() {
        WebsiteSearchResponse response = new WebsiteSearchResponse();
        response.setStatus(STATUS_SUCCESS);
        return response;
    }

    public List<WebsiteResponse> getData() {
        return data;
    }

    public void setData(List<WebsiteResponse> data) {
        this.data = data;
    }

}

