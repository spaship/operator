package io.websitecd.content.git.config;

import io.websitecd.content.git.config.model.ContentConfig;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.ComponentSpec;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class GitContentUtils {

    public static ContentConfig createConfig(String targetEnv, WebsiteConfig websiteConfig, String rootContext) {
        ContentConfig config = new ContentConfig();
        if (!OperatorConfigUtils.isEnvEnabled(websiteConfig, targetEnv)) {
            return config;
        }
        for (ComponentConfig c : websiteConfig.getComponents()) {
            ComponentSpec spec = c.getSpec();
            Map<String, Map<String, Object>> envs = spec.getEnvs();
            if (!OperatorConfigUtils.isEnvIncluded(envs, targetEnv)) {
                continue;
            }

            if (c.getKind().equals("git")) {
                String dir = c.getContext();
                if (StringUtils.equals("/", c.getContext())) {
                    dir = rootContext;
                }
                dir = dir.substring(1); // remove starting "/"
                String gitDir = StringUtils.defaultIfEmpty(spec.getDir(), "/");
                config.addGitComponent(dir, c.getKind(), spec.getUrl(), getRef(envs, targetEnv), gitDir);
            }
        }
        return config;
    }

    public static String getRef(Map<String, Map<String, Object>> envs, String targetEnv) {
        if (envs == null) {
            return targetEnv;
        }
        Map<String, Object> env = envs.get(targetEnv);
        if (env == null) {
            // no override - branch is same as env
            return targetEnv;
        }
        return (String) env.get("branch");
    }


}
