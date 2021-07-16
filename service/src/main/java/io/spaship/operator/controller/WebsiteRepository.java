package io.spaship.operator.controller;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.spaship.operator.crd.Website;
import io.spaship.operator.crd.WebsiteSpec;
import io.spaship.operator.crd.WebsiteStatus;
import io.spaship.operator.utility.HmacSHA256HashValidator;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.stream.Stream;

@ApplicationScoped
public class WebsiteRepository {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(WebsiteRepository.class);

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
        website.setStatus(new WebsiteStatus());

        return website;
    }

    public List<Website> getByGitUrl(String gitUrl, String secretToken, boolean sha256Hex) {
        LOG.debug("Get Websites by gitUrl gitUrl {}", gitUrl);
        List<Website> result = new ArrayList<>();
        for (Map.Entry<String, Website> entry : websites.entrySet()) {
            WebsiteSpec spec = entry.getValue().getSpec();
            if (gitUrl.equals(spec.getGitUrl()) && tokensSame(spec.getSecretToken(), secretToken, sha256Hex)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }


    public List<Website> getByGitUrl(String gitUrl, String message ,String gitHubHmacHash) {
        LOG.debug("Get Websites by gitUrl gitUrl {}", gitUrl);
        List<Website> result = new ArrayList<>();
        for (Map.Entry<String, Website> entry : websites.entrySet()) {
            WebsiteSpec spec = entry.getValue().getSpec();
            boolean isValidHash = HmacSHA256HashValidator.generateHash(message,spec.getSecretToken())
                    .apply(gitHubHmacHash);
            LOG.debug("hash match status {}",isValidHash);
            if (gitUrl.equals(spec.getGitUrl()) &&  isValidHash  ) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    public Stream<Website> searchWebsite(Optional<String> namespace, Optional<String> name) {
        return websites.values().stream()
                .filter(website -> {
                    if (namespace.isPresent() && !StringUtils.equals(website.getMetadata().getNamespace(), namespace.get())) {
                        return false;
                    }
                    if (name.isPresent() && !StringUtils.equals(website.getMetadata().getName(), name.get())) {
                        return false;
                    }
                    return true;
                });
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
