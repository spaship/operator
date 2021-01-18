package io.websitecd.content.git.config.model;

import java.util.LinkedList;
import java.util.List;

public class ContentConfig {
    List<GitComponent> components = new LinkedList<>();

    public void addComponent(String dir, String kind, GitSpec spec) {
        components.add(new GitComponent(dir, kind, spec));
    }

    public void addGitComponent(String dir, String kind, String gitUrl, String gitRef, String gitDir) {
        components.add(new GitComponent(dir, kind, gitUrl, gitRef, gitDir));
    }

    public List<GitComponent> getComponents() {
        return components;
    }

    public void setComponents(List<GitComponent> components) {
        this.components = components;
    }
}
