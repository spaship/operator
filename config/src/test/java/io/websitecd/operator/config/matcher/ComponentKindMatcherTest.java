package io.websitecd.operator.config.matcher;

import io.websitecd.operator.config.model.ComponentConfig;
import org.junit.Test;

import static io.websitecd.operator.config.matcher.ComponentKindMatcher.ComponentGitMatcher;
import static io.websitecd.operator.config.matcher.ComponentKindMatcher.ComponentServiceMatcher;
import static org.junit.Assert.assertTrue;

public class ComponentKindMatcherTest {

    @Test
    public void test() {
        assertTrue(ComponentGitMatcher.test(new ComponentConfig(null, ComponentConfig.KIND_GIT, null)));
        assertTrue(ComponentServiceMatcher.test(new ComponentConfig(null, ComponentConfig.KIND_SERVICE, null)));
        assertTrue(new ComponentKindMatcher("other_kind").test(new ComponentConfig(null, "other_kind", null)));
    }

}