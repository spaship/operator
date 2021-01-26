package io.websitecd.content.git.config;

import io.websitecd.content.git.config.model.ContentConfig;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.ComponentSpec;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.Map;

public class GitContentUtils {

    public static ContentConfig loadYaml(InputStream is) {
        Yaml yaml = new Yaml(new Constructor(ContentConfig.class));
        return yaml.load(is);
    }

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
                String dir = getDirName(c.getContext(), rootContext);
                config.addGitComponent(dir, c.getKind(), spec.getUrl(), getRef(envs, targetEnv));
            }
        }
        return config;
    }

    public static String getDirName(String context, String rootContext) {
        if (StringUtils.equals("/", context)) {
            return rootContext.substring(1);
        }
        return context.substring(1);
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
