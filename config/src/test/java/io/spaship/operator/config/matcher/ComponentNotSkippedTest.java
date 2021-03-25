package io.spaship.operator.config.matcher;

import io.spaship.operator.config.model.ComponentConfig;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ComponentNotSkippedTest {

    @Test
    public void test() {
        ComponentConfig componentNotSkip = new ComponentConfig("/notskip", null, null);
        ComponentConfig componentSkip = new ComponentConfig("/skip", null, null);

        Set<String> skipped = Set.of("/skip");
        assertTrue(new ComponentNotSkipped(skipped).test(componentNotSkip));
        assertFalse(new ComponentNotSkipped(skipped).test(componentSkip));
    }

}