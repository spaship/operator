package io.spaship.operator;

import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.crd.Website;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static Map<String, String> defaultLabels(String env, Website website) {
        WebsiteConfig config = website.getConfig();
        Map<String, String> labels = new HashMap<>();
        labels.put("website", website.getMetadata().getName());
        labels.put("env", env);
        labels.put("managedBy", "spaship-operator");
        if (config.getLabels() != null) {
            labels.putAll(config.getLabels());
        }
        return labels;
    }

    public static String getWebsiteName(Website website) {
        return website.getMetadata().getName();
    }

}
