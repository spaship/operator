package io.websitecd.operator.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.websitecd.operator.config.model.WebsiteConfig;

@Version("v1")
@Group("websitecd.io")
public class Website extends CustomResource<WebsiteSpec, WebsiteStatus> implements Namespaced {

    public Website() {
    }

    public Website(WebsiteConfig config) {
        this.config = config;
    }

    private WebsiteConfig config;

    public String getId() {
        return getMetadata().getNamespace() + "-" + getMetadata().getName();
    }

    public boolean specEquals(WebsiteSpec otherSpec) {
        return getSpec().equals(otherSpec);
    }

    public WebsiteConfig getConfig() {
        return config;
    }

    public void setConfig(WebsiteConfig config) {
        this.config = config;
    }
}
