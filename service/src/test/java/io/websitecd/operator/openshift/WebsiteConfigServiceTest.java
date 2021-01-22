package io.websitecd.operator.openshift;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebsiteConfigServiceTest {


    @Test
    public void getGitDirName() {
        assertEquals("root/https_gitlab_cee_redhat_comlkrzyzanecosystem-catalog-content_git",
                WebsiteConfigService.getGitDirName("root", "https://gitlab.cee.redhat.com/lkrzyzan/ecosystem-catalog-content.git"));
    }

}