package io.spaship.operator.websiteconfig;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class GitInfo {
    String branch;
    String dir;
    String configDir;

    public GitInfo(String branch, String dir, String configDir) {
        this.branch = branch;
        this.dir = dir;
        this.configDir = configDir;
    }

    public String getBranch() {
        return branch;
    }

    public String getDir() {
        return dir;
    }

    public String getConfigDir() {
        return configDir;
    }
}
