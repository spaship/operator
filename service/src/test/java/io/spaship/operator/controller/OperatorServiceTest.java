package io.spaship.operator.controller;

import io.spaship.operator.crd.Website;
import io.spaship.operator.crd.WebsiteEnvs;
import io.spaship.operator.crd.WebsiteSpec;
import io.spaship.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OperatorServiceTest {

    @Test
    public void createWebsiteCopy() {
        Website website = WebhookTestCommon.SIMPLE_WEBSITE;
        website.getMetadata().setName("simple");
        WebsiteSpec websiteSpec = website.getSpec();
        websiteSpec.setPreviews(true);

        Website websiteCopy = OperatorService.createWebsiteCopy(website, "previewId", "previewGit", "previewRef");
        WebsiteSpec specCopy = websiteCopy.getSpec();
        assertEquals("simple-pr-previewId", websiteCopy.getMetadata().getName());
        assertEquals(website.getMetadata().getNamespace(), websiteCopy.getMetadata().getNamespace());
        assertFalse(specCopy.getPreviews());
        assertEquals(" - Fork", specCopy.getDisplayName());
        assertEquals("previewGit", specCopy.getGitUrl());
        assertEquals("previewRef", specCopy.getBranch());
        assertEquals(websiteSpec.getDir(), specCopy.getDir());
        assertEquals(new WebsiteEnvs(List.of(), List.of()), specCopy.getEnvs());
        assertEquals(websiteSpec.getSecretToken(), specCopy.getSecretToken());
        assertEquals(websiteSpec.getSslVerify(), specCopy.getSslVerify());
        assertEquals("simple", websiteCopy.getMetadata().getLabels().get("websiteFork"));
        assertNull(websiteCopy.getStatus());
    }
}