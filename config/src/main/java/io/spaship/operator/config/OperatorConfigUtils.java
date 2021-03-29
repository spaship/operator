package io.spaship.operator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spaship.operator.config.model.ComponentConfig;
import io.spaship.operator.config.model.ComponentSpec;
import io.spaship.operator.config.model.Environment;
import io.spaship.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static io.spaship.operator.config.matcher.ComponentKindMatcher.ComponentGitMatcher;

public class OperatorConfigUtils {

    public static WebsiteConfig loadYaml(InputStream is) {
        Yaml yaml = new Yaml(new Constructor(WebsiteConfig.class));
        WebsiteConfig c = yaml.load(is);
        return c;
    }

    public static Map<String, Environment> getContentEnvsJson(InputStream contentEnvsJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        WebsiteConfig wrapper = mapper.readValue(contentEnvsJson, WebsiteConfig.class);
        return wrapper.getEnvs() == null ? Map.of() : wrapper.getEnvs();
    }

    public static Map<String, Environment> getContentEnvsJson(String json) throws IOException {
        if (StringUtils.isEmpty(json)) {
            return new HashMap<>();
        }
        return getContentEnvsJson(new ByteArrayInputStream(json.getBytes()));
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
