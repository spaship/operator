package io.websitecd.operator.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1")
@Group("websitecd.io")
public class Website extends CustomResource<WebsiteSpec, WebsiteStatus> implements Namespaced {


    public String getId() {
        return getMetadata().getNamespace() + "-" + getMetadata().getName();
    }

    public boolean specEquals(WebsiteSpec otherSpec) {
        return getSpec().equals(otherSpec);
    }
}
