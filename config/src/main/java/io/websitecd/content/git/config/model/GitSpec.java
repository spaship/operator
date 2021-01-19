package io.websitecd.content.git.config.model;

public class GitSpec {
    String url;
    String ref;
    String dir;

    public GitSpec() {
    }

    public GitSpec(String url, String ref, String dir) {
        this.url = url;
        this.ref = ref;
        this.dir = dir;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GitSpec{");
        sb.append("url='").append(url).append('\'');
        sb.append(", ref='").append(ref).append('\'');
        sb.append(", dir='").append(dir).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
