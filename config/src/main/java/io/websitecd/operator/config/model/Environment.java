package io.websitecd.operator.config.model;

import java.util.Set;

public class Environment {

    String namespace;
    String branch;
    Set<String> skipContexts;


    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Set<String> getSkipContexts() {
        return skipContexts;
    }

    public void setSkipContexts(Set<String> skipContexts) {
        this.skipContexts = skipContexts;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Environment{");
        sb.append("namespace='").append(namespace).append('\'');
        sb.append(", branch='").append(branch).append('\'');
        sb.append(", skipContexts=").append(skipContexts);
        sb.append('}');
        return sb.toString();
    }
}
