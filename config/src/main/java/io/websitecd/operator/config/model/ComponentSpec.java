package io.websitecd.operator.config.model;

import java.util.Map;

public class ComponentSpec {
    String url;
    String dir;
    String branch;

    String serviceName;
    String targetPort;

    Map<String, String> envs;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(String targetPort) {
        this.targetPort = targetPort;
    }

    public Map<String, String> getEnvs() {
        return envs;
    }

    public void setEnvs(Map<String, String> envs) {
        this.envs = envs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ComponentSpec{");
        sb.append("url='").append(url).append('\'');
        sb.append(", dir='").append(dir).append('\'');
        sb.append(", branch='").append(branch).append('\'');
        sb.append(", serviceName='").append(serviceName).append('\'');
        sb.append(", targetPort='").append(targetPort).append('\'');
        sb.append(", envs=").append(envs);
        sb.append('}');
        return sb.toString();
    }
}
