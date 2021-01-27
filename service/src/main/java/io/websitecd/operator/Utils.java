package io.websitecd.operator;

import io.websitecd.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static Map<String, String> defaultLabels(String env, WebsiteConfig config) {
        Map<String, String> labels = new HashMap<>();
        labels.put("env", env);
        if (config.getLabels() != null) {
            labels.putAll(config.getLabels());
        }
        return labels;
    }

    public static String getWebsiteName(WebsiteConfig config) {
        return StringUtils.defaultIfBlank(config.getWebsiteName(), "web");
    }

}
