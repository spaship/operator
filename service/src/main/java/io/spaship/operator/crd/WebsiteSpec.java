package io.spaship.operator.crd;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Objects;

@RegisterForReflection
public class WebsiteSpec {

    String displayName;
    String gitUrl;
    String branch;
    String dir;
    Boolean sslVerify = true;
    String secretToken;
    Boolean previews = false;
    String gitApiToken;
    WebsiteEnvs envs;

    public WebsiteSpec() {
    }

    public WebsiteSpec(String gitUrl, String branch, String dir, Boolean sslVerify, String secretToken) {
        this.gitUrl = gitUrl;
        this.branch = branch;
        this.dir = dir;
        this.sslVerify = sslVerify;
        this.secretToken = secretToken;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public Boolean getSslVerify() {
        return sslVerify;
    }

    public void setSslVerify(Boolean sslVerify) {
        this.sslVerify = sslVerify;
    }

    public String getSecretToken() {
        return secretToken;
    }

    public void setSecretToken(String secretToken) {
        this.secretToken = secretToken;
    }

    public Boolean getPreviews() {
        return previews;
    }

    public void setPreviews(Boolean previews) {
        this.previews = previews;
    }

    public String getGitApiToken() {
        return gitApiToken;
    }

    public void setGitApiToken(String gitApiToken) {
        this.gitApiToken = gitApiToken;
    }

    public WebsiteEnvs getEnvs() {
        return envs;
    }

    public void setEnvs(WebsiteEnvs envs) {
        this.envs = envs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebsiteSpec{");
        sb.append("displayName='").append(displayName).append('\'');
        sb.append(", gitUrl='").append(gitUrl).append('\'');
        sb.append(", branch='").append(branch).append('\'');
        sb.append(", dir='").append(dir).append('\'');
        sb.append(", sslVerify=").append(sslVerify);
        sb.append(", previews=").append(previews);
        sb.append(", envs=").append(envs);
        sb.append('}');
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebsiteSpec that = (WebsiteSpec) o;
        return Objects.equals(displayName, that.displayName) && gitUrl.equals(that.gitUrl) && Objects.equals(branch, that.branch) && Objects.equals(dir, that.dir) && Objects.equals(sslVerify, that.sslVerify) && Objects.equals(secretToken, that.secretToken) && Objects.equals(previews, that.previews) && Objects.equals(gitApiToken, that.gitApiToken) && Objects.equals(envs, that.envs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, gitUrl, branch, dir, sslVerify, secretToken, previews, gitApiToken, envs);
    }
}
