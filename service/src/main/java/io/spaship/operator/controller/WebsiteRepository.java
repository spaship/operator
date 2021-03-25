package io.spaship.operator.controller;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.spaship.operator.crd.Website;
import io.spaship.operator.crd.WebsiteSpec;
import org.apache.commons.codec.digest.DigestUtils;

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

    public void removeWebsite(String id) {
        websites.remove(id);
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

    public List<Website> getByGitUrl(String gitUrl, String secretToken, boolean sha256Hex) {
        List<Website> result = new ArrayList<>();
        for (Map.Entry<String, Website> entry : websites.entrySet()) {
            WebsiteSpec spec = entry.getValue().getSpec();
            if (gitUrl.equals(spec.getGitUrl()) && tokensSame(spec.getSecretToken(), secretToken, sha256Hex)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    boolean tokensSame(String websiteToken, String eventToken, boolean sha256Hex) {
        if (!sha256Hex) {
            return websiteToken.equals(eventToken);
        } else {
            return DigestUtils.sha256Hex(websiteToken).equals(eventToken);
        }
    }

    public Map<String, Website> getWebsites() {
        return websites;
    }

    public void reset() {
        websites = new HashMap<>();
    }


}
