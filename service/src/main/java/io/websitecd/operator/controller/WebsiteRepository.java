package io.websitecd.operator.controller;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.crd.WebsiteSpec;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class WebsiteRepository {

    private Map<String, Website> websites = new HashMap<>();

    public Website addWebsite(Website website) {
        String id = createId(website.getMetadata().getNamespace(), website.getMetadata().getName());
        websites.put(id, website);
        return website;
    }

    public Website getWebsite(String id) {
        return websites.get(id);
    }

    protected String createId(String namespace, String name) {
        return namespace + "-" + name;
    }

    public static Website createWebsite(String name, WebsiteSpec websiteSpec, String namespace) {
        Website website = new Website();
        website.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).build());
        website.setSpec(websiteSpec);

        return website;
    }

    public Website getByGitUrl(String gitUrl, String webhookSecretToken) {
        for (Map.Entry<String, Website> entry : websites.entrySet()) {
            WebsiteSpec spec = entry.getValue().getSpec();
            if (gitUrl.equals(spec.getGitUrl()) && webhookSecretToken.equals(spec.getWebhookSecret())) {
                return entry.getValue();
            }
        }
        return null;
    }



}
