package io.websitecd.operator.crd.matcher;

import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.crd.WebsiteEnvs;
import io.websitecd.operator.crd.WebsiteSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.websitecd.operator.crd.matcher.EnvIncluded.isEnvEnabled;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvIncludedTest {

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
        assertTrue(isEnvEnabled(new Website(), "something"));

        WebsiteEnvs envs = new WebsiteEnvs();
        website.getSpec().setEnvs(envs);

        assertTrue(isEnvEnabled(website,"something"));
    }

    @Test
    void isEnvIncluded() {
        website.getConfig().setEnvs(configEnvs);
        WebsiteEnvs envs = new WebsiteEnvs();
        website.getSpec().setEnvs(envs);

        envs.setIncluded(Arrays.asList("env1", "env2"));
        assertTrue(isEnvEnabled(website,"env1"));
        assertTrue(isEnvEnabled(website,"env2"));
        assertFalse(isEnvEnabled(website,"env3"));
    }

    @Test
    void isEnvIncludedRegexp() {
        website.getConfig().setEnvs(configEnvs);
        WebsiteEnvs envs = new WebsiteEnvs();
        website.getSpec().setEnvs(envs);

        envs.setIncluded(Arrays.asList(".*"));
        assertTrue(isEnvEnabled(website,"env1"));
        assertTrue(isEnvEnabled(website,"env2"));
        assertTrue(isEnvEnabled(website,"env3"));

        envs.setIncluded(Arrays.asList("env.*"));
        assertTrue(isEnvEnabled(website,"env1"));
        assertTrue(isEnvEnabled(website,"env2"));
        assertTrue(isEnvEnabled(website,"env3"));

        envs.setIncluded(Arrays.asList("somethingelse.*"));
        assertFalse(isEnvEnabled(website,"env1"));
        assertFalse(isEnvEnabled(website,"env2"));
        assertFalse(isEnvEnabled(website,"env3"));
    }

    @Test
    void isEnvExcluded() {
        website.getConfig().setEnvs(configEnvs);
        WebsiteEnvs envs = new WebsiteEnvs();
        website.getSpec().setEnvs(envs);

        envs.setExcluded(Arrays.asList("env2"));
        assertTrue(isEnvEnabled(website,"env1"));
        assertFalse(isEnvEnabled(website,"env2"));
        assertTrue(isEnvEnabled(website,"env3"));
    }

    @Test
    void isEnvExcludedRegexp() {
        website.getConfig().setEnvs(configEnvs);
        WebsiteEnvs envs = new WebsiteEnvs();
        website.getSpec().setEnvs(envs);

        envs.setExcluded(Arrays.asList(".*"));
        assertFalse(isEnvEnabled(website,"env1"));
        assertFalse(isEnvEnabled(website,"env2"));
        assertFalse(isEnvEnabled(website,"env3"));

        envs.setExcluded(Arrays.asList("env2.*"));
        assertTrue(isEnvEnabled(website,"env1"));
        assertFalse(isEnvEnabled(website,"env2"));
        assertTrue(isEnvEnabled(website,"env3"));

        envs.setExcluded(Arrays.asList("somethingelse.*"));
        assertTrue(isEnvEnabled(website,"env1"));
        assertTrue(isEnvEnabled(website,"env2"));
        assertTrue(isEnvEnabled(website,"env3"));
    }

}