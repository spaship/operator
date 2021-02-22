package io.websitecd.operator.crd;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebsiteStatus{");
        sb.append("status='").append(status).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", envs='").append(envs).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
