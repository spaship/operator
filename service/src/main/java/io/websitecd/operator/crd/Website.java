package io.websitecd.operator.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.websitecd.operator.config.model.WebsiteConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        if (config.getEnvs() != null && config.getEnvs().size() > 0) {
            this.enabledEnvs = getEnvNamesStream(config).collect(Collectors.toSet());
        } else {
            this.enabledEnvs = new HashSet<>();
        }
    }


    /* Helper methods */
    private Set<String> enabledEnvs;

    public Set<String> getEnabledEnvs() {
        return enabledEnvs;
    }

    private Stream<String> getEnvNamesStream(WebsiteConfig config) {
        return config.getEnvs().keySet().stream().filter(this::isEnvEnabled);
    }

    public boolean isEnvEnabled(String env) {
        if (getSpec() == null || getSpec().getEnvs() == null) {
            return true;
        }
        WebsiteEnvs envs = getSpec().getEnvs();
        if (envs.getIncluded() != null) {
            for (String include : envs.getIncluded()) {
                if (env.matches(include)) {
                    return true;
                }
            }
            return false;
        }
        if (envs.getExcluded() != null) {
            for (String exclude : envs.getExcluded()) {
                if (env.matches(exclude)) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }
}
