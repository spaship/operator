package io.spaship.operator.openshift;

import io.spaship.operator.websiteconfig.GitWebsiteConfigService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GitWebsiteConfigServiceTest {


    @Test
    public void getGitDirName() {
        assertEquals("root/git-website_https_gitlab_cee_redhat_comlkrzyzanecosystem-catalog-content_git",
                GitWebsiteConfigService.getGitDirName("root", "https://gitlab.cee.redhat.com/lkrzyzan/ecosystem-catalog-content.git"));
    }

}