package io.spaship.operator.config.model;

import java.util.Map;
import java.util.Objects;

public class ComponentSpec {
    String url;
    String dir;
    String branch;

    String serviceName;
    Integer targetPort;

    Map<String, String> envs;

    public static ComponentSpec createServiceSpec(String serviceName, Integer targetPort) {
        ComponentSpec spec = new ComponentSpec();
        spec.serviceName = serviceName;
        spec.targetPort = targetPort;
        return spec;
    }

    public static ComponentSpec createGitSpec(String url, String dir, String branch) {
        ComponentSpec spec = new ComponentSpec();
        spec.url = url;
        spec.dir = dir;
        spec.branch = branch;
        return spec;
    }

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

    public Integer getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(Integer targetPort) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentSpec that = (ComponentSpec) o;
        return Objects.equals(url, that.url) && Objects.equals(dir, that.dir) && Objects.equals(branch, that.branch) && Objects.equals(serviceName, that.serviceName) && Objects.equals(targetPort, that.targetPort) && Objects.equals(envs, that.envs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, dir, branch, serviceName, targetPort, envs);
    }
}
