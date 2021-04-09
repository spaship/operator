package io.spaship.operator.rest.website.model;

public class WebsiteEnvironment {

    String name;
    String domain;
    String api;

    public WebsiteEnvironment() {
    }

    public WebsiteEnvironment(String name, String domain, String api) {
        this.name = name;
        this.domain = domain;
        this.api = api;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }
}
