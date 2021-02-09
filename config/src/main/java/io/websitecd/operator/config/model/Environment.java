package io.websitecd.operator.config.model;

import java.util.Set;

public class Environment {

    String branch;
    Set<String> skipContexts;
    DeploymentConfig deployment;

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

    public DeploymentConfig getDeployment() {
        return deployment;
    }

    public void setDeployment(DeploymentConfig deployment) {
        this.deployment = deployment;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Environment{");
        sb.append("branch='").append(branch).append('\'');
        sb.append(", skipContexts=").append(skipContexts);
        sb.append(", deployment=").append(deployment);
        sb.append('}');
        return sb.toString();
    }
}
