package io.websitecd.operator.config;

import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.ComponentSpec;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

import static io.websitecd.operator.config.matcher.ComponentKindMatcher.ComponentGitMatcher;

public class OperatorConfigUtils {

    public static WebsiteConfig loadYaml(InputStream is) {
        Yaml yaml = new Yaml(new Constructor(WebsiteConfig.class));
        WebsiteConfig c = yaml.load(is);
        return c;
    }

    /**
     * Apply to given website config default git url for all "kind: git" components if missing
     *
     * @param config
     * @param gitUrl default git url
     * @return
     * @see ComponentSpec#setUrl(String)
     */
    public static int applyDefaultGirUrl(WebsiteConfig config, String gitUrl) {
        int applied = 0;
        for (ComponentConfig component : config.getComponents()) {
            if (!ComponentGitMatcher.test(component)) {
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

}
