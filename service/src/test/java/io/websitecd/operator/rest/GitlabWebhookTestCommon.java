package io.websitecd.operator.rest;

import io.websitecd.operator.QuarkusTestBase;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.controller.WebsiteRepository;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.crd.WebsiteSpec;
import io.websitecd.operator.openshift.GitWebsiteConfigService;
import io.websitecd.operator.openshift.OperatorService;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;

public class GitlabWebhookTestCommon extends QuarkusTestBase {

    public static final String GIT_EXAMPLES_URL = "https://github.com/websitecd/websitecd-examples.git";
    public static final String GIT_EXAMPLES_BRANCH = "main";
    public static final String GIT_EXAMPLES_CONFIG_SIMPLE = "websites/01-simple";
    public static final String GIT_EXAMPLES_CONFIG_ADVANCED = "websites/02-advanced";
    public static final String SECRET_SIMPLE = "testsecret_simple";
    public static final String SECRET_ADVANCED = "testsecret_advanced";

    public static WebsiteSpec SIMPLE_WEB = new WebsiteSpec(GIT_EXAMPLES_URL, GIT_EXAMPLES_BRANCH, GIT_EXAMPLES_CONFIG_SIMPLE, true, SECRET_SIMPLE);
    public static WebsiteSpec ADVANCED_WEB = new WebsiteSpec(GIT_EXAMPLES_URL, GIT_EXAMPLES_BRANCH, GIT_EXAMPLES_CONFIG_ADVANCED, true, SECRET_ADVANCED);

    public static Website SIMPLE_WEBSITE = WebsiteRepository.createWebsite("simple", SIMPLE_WEB, "websitecd-examples");
    public static Website ADVANCED_WEBSITE = WebsiteRepository.createWebsite("advanced", ADVANCED_WEB, "websitecd-examples");


    @Inject
    OperatorService operatorService;
    @Inject
    WebsiteRepository websiteRepository;
    @Inject
    GitWebsiteConfigService gitWebsiteConfigService;


    public void registerSimpleWeb() throws GitAPIException, IOException, URISyntaxException {
        websiteRepository.reset();
        WebsiteConfig websiteConfig = gitWebsiteConfigService.cloneRepo(SIMPLE_WEBSITE);
        SIMPLE_WEBSITE.setConfig(websiteConfig);
        websiteRepository.addWebsite(SIMPLE_WEBSITE);
        operatorService.initNewWebsite(SIMPLE_WEBSITE);
    }

    public void registerAdvancedWeb() throws GitAPIException, IOException, URISyntaxException {
        registerAdvancedWeb(true);
    }

    public void registerAdvancedWeb(boolean reset) throws GitAPIException, IOException, URISyntaxException {
        if (reset) {
            websiteRepository.reset();
        }
        websiteRepository.reset();
        WebsiteConfig websiteConfig = gitWebsiteConfigService.cloneRepo(ADVANCED_WEBSITE);
        ADVANCED_WEBSITE.setConfig(websiteConfig);
        websiteRepository.addWebsite(ADVANCED_WEBSITE);
        operatorService.initNewWebsite(ADVANCED_WEBSITE);
    }

}
