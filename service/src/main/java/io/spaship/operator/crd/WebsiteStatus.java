package io.spaship.operator.crd;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisterForReflection
public class WebsiteStatus {

    public static enum STATUS {
        GIT_CLONING,
        GIT_PULLING,
        CREATING,
        FAILED,
        DEPLOYED;
        @Override
        public String toString() {
            switch (this) {
                case GIT_CLONING:
                    return "Git Cloning";
                case GIT_PULLING:
                    return "Git Pulling";
                case CREATING:
                    return "Creating";
                case FAILED:
                    return "Failed";
                case DEPLOYED:
                    return "Deployed";
                default:
                    return this.toString();
            }
        }
    }

    String status;

    String message;

    List<String> envs;

    Map<String, String> envHosts;

    public WebsiteStatus() {
    }

    public WebsiteStatus(String status, String message, List<String> envs) {
        this.status = status;
        this.message = message;
        this.envs = envs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status.toString();
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getEnvs() {
        return envs;
    }

    public void setEnvs(List<String> envs) {
        this.envs = envs;
    }

    public Map<String, String> getEnvHosts() {
        return envHosts;
    }

    public void setEnvHosts(Map<String, String> envHosts) {
        this.envHosts = envHosts;
    }

    public void addEnvHost(String env, String host) {
        if (envHosts == null) {
            envHosts = new HashMap<>();
        }
        envHosts.put(env, host);
    }
    public String getHost(String env) {
        if (envHosts != null && envHosts.containsKey(env)) {
            return envHosts.get(env);
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebsiteStatus{");
        sb.append("status='").append(status).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", envs=").append(envs);
        sb.append(", envHosts='").append(envHosts).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
