package io.spaship.operator.config.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.spaship.operator.config.matcher.ComponentNotSkipped;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static io.spaship.operator.config.matcher.ComponentKindMatcher.ComponentGitMatcher;
import static io.spaship.operator.config.matcher.ComponentKindMatcher.ComponentServiceMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

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

    @JsonIgnore
    public Stream<ComponentConfig> getEnabledGitComponents(String targetEnv) {
        return getEnabledComponents(targetEnv).filter(ComponentGitMatcher);
    }
    @JsonIgnore
    public Stream<ComponentConfig> getEnabledServiceComponents(String targetEnv) {
        return getEnabledComponents(targetEnv).filter(ComponentServiceMatcher);
    }

    @JsonIgnore
    public Stream<ComponentConfig> getEnabledComponents(String targetEnv) {
        Environment env = getEnvironment(targetEnv);
        if (env != null) {
            return components.stream().filter(new ComponentNotSkipped(env.getSkipContexts()));
        } else {
            return Stream.empty();
        }
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
        String result = null;
        try {
            result = new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result;
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
