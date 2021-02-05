package io.websitecd.operator.crd;

public class WebsiteSpec {

    String gitUrl;
    String branch;
    String dir;
    String sslVerify;
    String webhookSecret;

    public WebsiteSpec() {
    }

    public WebsiteSpec(String gitUrl, String branch, String dir) {
        this.gitUrl = gitUrl;
        this.branch = branch;
        this.dir = dir;
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

    public String getSslVerify() {
        return sslVerify;
    }

    public void setSslVerify(String sslVerify) {
        this.sslVerify = sslVerify;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebsiteSpec{");
        sb.append("gitUrl='").append(gitUrl).append('\'');
        sb.append(", branch='").append(branch).append('\'');
        sb.append(", dir='").append(dir).append('\'');
        sb.append(", sslVerify='").append(sslVerify).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
