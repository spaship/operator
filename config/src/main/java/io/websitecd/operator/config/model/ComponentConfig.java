package io.websitecd.operator.config.model;

public class ComponentConfig {
    String context;
    String kind;
    ComponentSpec spec;

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public boolean isKindGit() {
        return "git".equals(kind);
    }

    public boolean isKindService() {
        return "service".equals(kind);
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
}
