package io.websitecd.operator.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.crd.matcher.EnvIncluded;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Version("v1")
@Group("websitecd.io")
@RegisterForReflection
public class Website extends CustomResource<WebsiteSpec, WebsiteStatus> implements Namespaced {

    public Website() {
    }

    public Website(WebsiteConfig config) {
        this.config = config;
    }

    private WebsiteConfig config;

    public String getId() {
        return createId(getMetadata().getNamespace(), getMetadata().getName());
    }
    public static String createId(String namespace, String name) {
        return namespace + "-" + name;
    }

    public boolean specEquals(WebsiteSpec otherSpec) {
        return getSpec().equals(otherSpec);
    }

    public WebsiteConfig getConfig() {
        return config;
    }

    public void setConfig(WebsiteConfig config) {
        this.config = config;
        if (config.getEnvs() != null && config.getEnvs().size() > 0) {
            this.enabledEnvs = config.getEnvs().keySet().stream()
                    .filter(new EnvIncluded(this))
                    .collect(Collectors.toSet());
        } else {
            this.enabledEnvs = new HashSet<>();
        }
    }

    /* Helper methods */
    private Set<String> enabledEnvs;

    public Set<String> getEnabledEnvs() {
        return enabledEnvs;
    }

}
