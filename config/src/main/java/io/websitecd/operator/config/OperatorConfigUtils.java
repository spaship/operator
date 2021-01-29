package io.websitecd.operator.config;

import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.ComponentSpec;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.Set;

public class OperatorConfigUtils {

    public static WebsiteConfig loadYaml(InputStream is) {
        Yaml yaml = new Yaml(new Constructor(WebsiteConfig.class));
        WebsiteConfig c = yaml.load(is);
        return c;
    }

    public static int applyDefaultGirUrl(WebsiteConfig config, String gitUrl) {
        int applied = 0;
        for (ComponentConfig component : config.getComponents()) {
            if (!component.isKindGit()) {
                continue;
            }
            ComponentSpec spec = component.getSpec();
            if (StringUtils.isEmpty(spec.getUrl())) {
                spec.setUrl(gitUrl);
                applied++;
            }
        }
        return applied;
    }

    public static boolean isEnvEnabled(WebsiteConfig config, String targetEnv) {
        return config.getEnvs().containsKey(targetEnv);
    }


    public static boolean isComponentEnabled(WebsiteConfig config, String targetEnv, String context) {
        Set<String> skipContexts = config.getEnvironment(targetEnv).getSkipContexts();
        if (skipContexts != null && skipContexts.contains(context)) {
            return false;
        }
        return true;
    }

}
