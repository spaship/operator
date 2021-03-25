package io.spaship.operator.crd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.crd.matcher.EnvIncluded;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Version("v1")
@Group("spaship.io")
@RegisterForReflection
public class Website extends CustomResource<WebsiteSpec, WebsiteStatus> implements Namespaced {

    public Website() {
    }

    @JsonIgnore
    private WebsiteConfig config;

    @JsonIgnore
    public String getId() {
        return createId(getMetadata().getNamespace(), getMetadata().getName());
    }

    @JsonIgnore
    public static String createId(String namespace, String name) {
        return namespace + "-" + name;
    }

    @JsonIgnore
    public WebsiteConfig getConfig() {
        return config;
    }

    @JsonIgnore
    public void setConfig(WebsiteConfig config) {
        this.config = config;
        if (config!= null && config.getEnvs() != null && config.getEnvs().size() > 0) {
            this.enabledEnvs = config.getEnvs().keySet().stream()
                    .filter(new EnvIncluded(this))
                    .collect(Collectors.toSet());
        } else {
            this.enabledEnvs = new HashSet<>();
        }
    }

    /* Helper methods */
    @JsonIgnore
    private Set<String> enabledEnvs;

    @JsonIgnore
    public Set<String> getEnabledEnvs() {
        return enabledEnvs;
    }

}
