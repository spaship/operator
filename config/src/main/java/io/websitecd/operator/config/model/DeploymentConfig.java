package io.websitecd.operator.config.model;

import io.fabric8.kubernetes.api.model.Container;

public class DeploymentConfig {
    Integer replicas;
    Container init;
    Container httpd;
    Container api;

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public Container getInit() {
        return init;
    }

    public void setInit(Container init) {
        this.init = init;
    }

    public Container getHttpd() {
        return httpd;
    }

    public void setHttpd(Container httpd) {
        this.httpd = httpd;
    }

    public Container getApi() {
        return api;
    }

    public void setApi(Container api) {
        this.api = api;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DeploymentConfig{");
        sb.append("replicas='").append(replicas).append('\'');
        sb.append(", init=").append(init);
        sb.append(", httpd=").append(httpd);
        sb.append(", api=").append(api);
        sb.append('}');
        return sb.toString();
    }
}
