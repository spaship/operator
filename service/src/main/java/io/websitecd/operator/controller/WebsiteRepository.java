package io.websitecd.operator.controller;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.crd.WebsiteSpec;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class WebsiteRepository {

    private Map<String, Website> websites = new HashMap<>();

    public Website addWebsite(Website website) {
        if (website.getConfig() == null) {
            throw new RuntimeException("Cannot register new website without configuration");
        }
        websites.put(website.getId(), website);
        return website;
    }

    public void removeWebsite(Website website) {
        websites.remove(website.getId());
    }

    public Website getWebsite(String id) {
        return websites.get(id);
    }

    public static Website createWebsite(String name, WebsiteSpec websiteSpec, String namespace) {
        Website website = new Website();
        website.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).build());
        website.setSpec(websiteSpec);

        return website;
    }

    public List<Website> getByGitUrl(String gitUrl, String webhookSecretToken) {
        List<Website> result = new ArrayList<>();
        for (Map.Entry<String, Website> entry : websites.entrySet()) {
            WebsiteSpec spec = entry.getValue().getSpec();
            if (gitUrl.equals(spec.getGitUrl()) && webhookSecretToken.equals(spec.getWebhookSecret())) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    public Map<String, Website> getWebsites() {
        return websites;
    }

    public void reset() {
        websites = new HashMap<>();
    }


}
