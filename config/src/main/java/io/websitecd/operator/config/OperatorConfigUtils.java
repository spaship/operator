package io.websitecd.operator.config;

import io.websitecd.operator.config.model.WebsiteConfig;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.Map;

public class OperatorConfigUtils {

    public static WebsiteConfig loadYaml(InputStream is) {
        Yaml yaml = new Yaml(new Constructor(WebsiteConfig.class));
        WebsiteConfig c = yaml.load(is);
        return c;
    }

    public static boolean isEnvEnabled(WebsiteConfig config, String targetEnv) {
        return config.getEnvs().containsKey(targetEnv);
    }


    /**
     * Check if environment is included in "spec.envs" array.
     *
     * @param envs
     * @param targetEnv
     * @return
     */
    public static boolean isEnvIncluded(Map<String, Map<String, Object>> envs, String targetEnv) {
        if (envs == null) {
            return true;
        }
        return envs.containsKey(targetEnv);
    }


}
