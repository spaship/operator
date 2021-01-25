package io.websitecd.operator.content;

import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.ComponentSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentControllerTest {

    @Test
    void getAliasPath() {
        ComponentSpec spec1 = new ComponentSpec();
        spec1.setDir("/subdir");

        assertEquals("/_root/spa1/subdir/", ContentController.getAliasPath("/_root", new ComponentConfig("/spa1", "git", spec1)));
        assertEquals("/_root/spa1/subdir/", ContentController.getAliasPath("/_root", new ComponentConfig("/spa1/", "git", spec1)));

        ComponentSpec rootSpec = new ComponentSpec();
        rootSpec.setDir("/");

        assertEquals("/_root/spa2/", ContentController.getAliasPath("/_root", new ComponentConfig("/spa2", "git", rootSpec)));

        assertEquals("/_root/spa2/", ContentController.getAliasPath("/_root", new ComponentConfig("/spa2", "git", new ComponentSpec())));
    }
}