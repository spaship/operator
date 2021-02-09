package io.websitecd.operator.crd;

import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebsiteTest {

    Map<String, Environment> configEnvs = new HashMap<>();
    Website website;

    @BeforeEach
    void setupEnvs() {
        website = new Website();
        WebsiteSpec websiteSpec = new WebsiteSpec();
        website.setSpec(websiteSpec);
        website.setConfig(new WebsiteConfig());

        configEnvs.put("env1", new Environment());
        configEnvs.put("env2", new Environment());
        configEnvs.put("env3", new Environment());
    }

    @Test
    void testDefaults() {
        assertTrue(new Website().isEnvEnabled("something"));

        WebsiteEnvs envs = new WebsiteEnvs();
        website.getSpec().setEnvs(envs);

        assertTrue(website.isEnvEnabled("something"));
    }

    @Test
    void isEnvIncluded() {
        website.getConfig().setEnvs(configEnvs);
        WebsiteEnvs envs = new WebsiteEnvs();
        website.getSpec().setEnvs(envs);

        envs.setIncluded(Arrays.asList("env1", "env2"));
        assertTrue(website.isEnvEnabled("env1"));
        assertTrue(website.isEnvEnabled("env2"));
        assertFalse(website.isEnvEnabled("env3"));
    }

    @Test
    void isEnvIncludedRegexp() {
        website.getConfig().setEnvs(configEnvs);
        WebsiteEnvs envs = new WebsiteEnvs();
        website.getSpec().setEnvs(envs);

        envs.setIncluded(Arrays.asList(".*"));
        assertTrue(website.isEnvEnabled("env1"));
        assertTrue(website.isEnvEnabled("env2"));
        assertTrue(website.isEnvEnabled("env3"));

        envs.setIncluded(Arrays.asList("env.*"));
        assertTrue(website.isEnvEnabled("env1"));
        assertTrue(website.isEnvEnabled("env2"));
        assertTrue(website.isEnvEnabled("env3"));

        envs.setIncluded(Arrays.asList("somethingelse.*"));
        assertFalse(website.isEnvEnabled("env1"));
        assertFalse(website.isEnvEnabled("env2"));
        assertFalse(website.isEnvEnabled("env3"));
    }

    @Test
    void isEnvExcluded() {
        website.getConfig().setEnvs(configEnvs);
        WebsiteEnvs envs = new WebsiteEnvs();
        website.getSpec().setEnvs(envs);

        envs.setExcluded(Arrays.asList("env2"));
        assertTrue(website.isEnvEnabled("env1"));
        assertFalse(website.isEnvEnabled("env2"));
        assertTrue(website.isEnvEnabled("env3"));
    }

    @Test
    void isEnvExcludedRegexp() {
        website.getConfig().setEnvs(configEnvs);
        WebsiteEnvs envs = new WebsiteEnvs();
        website.getSpec().setEnvs(envs);

        envs.setExcluded(Arrays.asList(".*"));
        assertFalse(website.isEnvEnabled("env1"));
        assertFalse(website.isEnvEnabled("env2"));
        assertFalse(website.isEnvEnabled("env3"));

        envs.setExcluded(Arrays.asList("env2.*"));
        assertTrue(website.isEnvEnabled("env1"));
        assertFalse(website.isEnvEnabled("env2"));
        assertTrue(website.isEnvEnabled("env3"));

        envs.setExcluded(Arrays.asList("somethingelse.*"));
        assertTrue(website.isEnvEnabled("env1"));
        assertTrue(website.isEnvEnabled("env2"));
        assertTrue(website.isEnvEnabled("env3"));
    }

}