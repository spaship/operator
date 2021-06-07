package io.spaship.operator.rest.website.model;

import io.spaship.operator.crd.Website;
import io.spaship.operator.rest.website.WebsiteResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebsiteResponse {
    String name;

    WebsiteSpec spec;

    List<WebsiteEnvironment> environments;

    public static WebsiteResponse createFromCrd(Website crd, Optional<String> operatorUrl) {
        String namespace = crd.getMetadata().getNamespace();
        String name = crd.getMetadata().getName();

        WebsiteResponse result = new WebsiteResponse();
        result.setName(crd.getSpec().getDisplayName());
        result.setSpec(new WebsiteSpec(namespace, name));

        List<WebsiteEnvironment> environments = new ArrayList<>();
        for (String env : crd.getEnabledEnvs()) {
            String api = null;
            if (operatorUrl.isPresent()) {
                api = operatorUrl.get() + WebsiteResource.getApplicationApiPath(namespace, name, env);
            }
            environments.add(new WebsiteEnvironment(env, crd.getStatus().getHost(env), api));
        }
        result.setEnvironments(environments);
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WebsiteSpec getSpec() {
        return spec;
    }

    public void setSpec(WebsiteSpec spec) {
        this.spec = spec;
    }

    public List<WebsiteEnvironment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<WebsiteEnvironment> environments) {
        this.environments = environments;
    }
}
