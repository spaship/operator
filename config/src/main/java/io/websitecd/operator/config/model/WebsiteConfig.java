package io.websitecd.operator.config.model;

import io.websitecd.operator.config.OperatorConfigUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class WebsiteConfig {

    String apiVersion;

    Map<String, String> metadata;

    Map<String, String> labels;

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

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public ComponentConfig getComponent(String context) {
        for (ComponentConfig component : components) {
            if (StringUtils.equals(component.getContext(), context)) {
                return component;
            }
        }
        return null;
    }

    public Stream<ComponentConfig> getEnabledGitComponents(String targetEnv) {
        return getEnabledComponents(targetEnv).filter(ComponentConfig::isKindGit);
    }

    public Stream<ComponentConfig> getEnabledComponents(String targetEnv) {
        Environment env = getEnvironment(targetEnv);
        if (env != null && env.getSkipContexts() != null && env.getSkipContexts().size() > 0) {
            return components.stream()
                    .filter(c -> OperatorConfigUtils.isComponentEnabled(this, targetEnv, c));
        }
        return components.stream();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebsiteConfig that = (WebsiteConfig) o;
        return Objects.equals(apiVersion, that.apiVersion) && Objects.equals(metadata, that.metadata) && Objects.equals(labels, that.labels) && Objects.equals(envs, that.envs) && Objects.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiVersion, metadata, labels, envs, components);
    }
}
