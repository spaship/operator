package io.websitecd.operator.config.model;

import java.util.Map;

public class ComponentSpec {
    String url;
    String dir;

    String serviceName;
    String targetPort;

    Map<String, Map<String, Object>> envs;

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

    public Map<String, Map<String, Object>> getEnvs() {
        return envs;
    }

    public void setEnvs(Map<String, Map<String, Object>> envs) {
        this.envs = envs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ComponentSpec{");
        sb.append("url='").append(url).append('\'');
        sb.append(", dir='").append(dir).append('\'');
        sb.append(", serviceName='").append(serviceName).append('\'');
        sb.append(", targetPort='").append(targetPort).append('\'');
        sb.append(", envs=").append(envs);
        sb.append('}');
        return sb.toString();
    }
}
