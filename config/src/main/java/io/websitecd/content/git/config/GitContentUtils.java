package io.websitecd.content.git.config;

import io.websitecd.content.git.config.model.ContentConfig;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.ComponentSpec;
import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

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
            if (!OperatorConfigUtils.isComponentEnabled(websiteConfig, targetEnv, c.getContext())) {
                continue;
            }

            if (c.getKind().equals("git")) {
                String dir = getDirName(c.getContext(), rootContext);
                config.addGitComponent(dir, c.getKind(), spec.getUrl(), getRef(websiteConfig, targetEnv, c.getContext()));
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


    public static String getRef(WebsiteConfig config, String targetEnv, String context) {
        String ref = null;
        Environment env = config.getEnvironment(targetEnv);
        if (env != null) {
            ref = env.getBranch();
        }
        ComponentSpec spec = config.getComponent(context).getSpec();
        if (StringUtils.isNotEmpty(spec.getBranch())) {
            ref = spec.getBranch();
        }
        if (spec.getEnvs() != null && StringUtils.isNotEmpty(spec.getEnvs().get(targetEnv))) {
            ref = spec.getEnvs().get(targetEnv);
        }

        return ref;
    }


}
