package io.spaship.operator.crd;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisterForReflection
public class WebsiteStatus {

    String status;
    String message;
    List<String> envs;
    Map<String, String> envHosts;
    String updated;

    public WebsiteStatus() {
    }

    public WebsiteStatus(String status, String message, List<String> envs) {
        this.status = status;
        this.message = message;
        this.envs = envs;
    }

    public WebsiteStatus(String updated) {
        this.updated = updated;
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

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebsiteStatus{");
        sb.append("status='").append(status).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", envs=").append(envs);
        sb.append(", envHosts=").append(envHosts);
        sb.append(", updated='").append(updated).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public enum STATUS {
        FORCE_UPDATE,
        GIT_CLONING,
        GIT_PULLING,
        FAILED,
        DEPLOYED;

        @Override
        public String toString() {
            switch (this) {
                case FORCE_UPDATE:
                    return "Force Update";
                case GIT_CLONING:
                    return "Git Cloning";
                case GIT_PULLING:
                    return "Git Pulling";
                case FAILED:
                    return "Failed";
                case DEPLOYED:
                    return "Deployed";
                default:
                    return super.toString();
            }
        }

        public boolean equalsTo(String otherStatus) {
            return StringUtils.equalsIgnoreCase(this.toString(), otherStatus);
        }
    }
}
