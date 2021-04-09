package io.spaship.operator.rest.website.model;

public class WebsiteSpec {
    String namespace;
    String name;

    public WebsiteSpec(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public WebsiteSpec() {
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
