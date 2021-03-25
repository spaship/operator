package io.spaship.content.git.config.model;

import java.util.LinkedList;
import java.util.List;

public class ContentConfig {
    List<GitComponent> components = new LinkedList<>();

    public void addComponent(String dir, String kind, GitSpec spec) {
        components.add(new GitComponent(dir, kind, spec));
    }

    public void addGitComponent(String dir, String kind, String gitUrl, String gitRef) {
        components.add(new GitComponent(dir, kind, gitUrl, gitRef));
    }

    public List<GitComponent> getComponents() {
        return components;
    }

    public void setComponents(List<GitComponent> components) {
        this.components = components;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ContentConfig{");
        sb.append("components=").append(components);
        sb.append('}');
        return sb.toString();
    }
}
