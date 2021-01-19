package io.websitecd.operator.config.model;

public class Environment {

    String namespace;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Environment{");
        sb.append("namespace='").append(namespace).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
