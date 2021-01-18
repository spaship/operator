package io.websitecd.operator.config.model;

import java.util.List;
import java.util.Map;

public class WebsiteConfig {

    String apiVersion;

    Map<String, Environment> envs;

    List<ComponentConfig> components;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public List<ComponentConfig> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentConfig> components) {
        this.components = components;
    }

    public Map<String, Environment> getEnvs() {
        return envs;
    }

    public void setEnvs(Map<String, Environment> envs) {
        this.envs = envs;
    }

    public Environment getEnvironment(String envName) {
        return envs.get(envName);
    }
}
