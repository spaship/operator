package io.spaship.operator.config.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

public class ComponentConfig {

    public static final String KIND_GIT = "git";
    public static final String KIND_SERVICE = "service";

    String context;
    String kind;
    ComponentSpec spec;

    public ComponentConfig() {
    }

    public ComponentConfig(String context, String kind, ComponentSpec spec) {
        this.context = context;
        this.kind = kind;
        this.spec = spec;
    }

    public String getContext() {
        return context;
    }

    public String getComponentName() {
        return getContext().substring(1);  // remove starting /
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public ComponentSpec getSpec() {
        return spec;
    }

    public void setSpec(ComponentSpec spec) {
        this.spec = spec;
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
        ComponentConfig that = (ComponentConfig) o;
        return Objects.equals(context, that.context) && Objects.equals(kind, that.kind) && Objects.equals(spec, that.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, kind, spec);
    }
}
