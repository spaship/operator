package io.websitecd.operator.config.model;

import java.util.List;
import java.util.Map;

public class WebsiteConfig {

    String apiVersion;

    Map<String, String> metadata;

    Map<String, Environment> envs;

    List<ComponentConfig> components;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getWebsiteName() {
        if (metadata != null && metadata.containsKey("name")) {
            return metadata.get("name");
        }
        return null;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebsiteConfig{");
        sb.append("apiVersion='").append(apiVersion).append('\'');
        sb.append(", metadata=").append(metadata);
        sb.append(", envs=").append(envs);
        sb.append(", components=").append(components);
        sb.append('}');
        return sb.toString();
    }
}
