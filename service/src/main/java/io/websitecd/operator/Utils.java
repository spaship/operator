package io.websitecd.operator;

import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.crd.Website;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static Map<String, String> defaultLabels(String env, Website website) {
        WebsiteConfig config = website.getConfig();
        Map<String, String> labels = new HashMap<>();
        labels.put("website", website.getMetadata().getName());
        labels.put("env", env);
        labels.put("managedBy", "websitecd-operator");
        if (config.getLabels() != null) {
            labels.putAll(config.getLabels());
        }
        return labels;
    }

    public static String getWebsiteName(Website website) {
        return website.getMetadata().getName();
    }

}
