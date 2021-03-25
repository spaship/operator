package io.spaship.content.git.config.model;

public class GitSpec {
    String url;
    String ref;

    public GitSpec() {
    }

    public GitSpec(String url, String ref) {
        this.url = url;
        this.ref = ref;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GitSpec{");
        sb.append("url='").append(url).append('\'');
        sb.append(", ref='").append(ref).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
