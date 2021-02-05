package io.websitecd.operator.rest;

import io.websitecd.operator.controller.WebsiteRepository;
import io.websitecd.operator.openshift.OperatorService;
import io.websitecd.operator.openshift.OperatorServiceTest;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;

public class GitlabWebhookTestCommon {

    @Inject
    OperatorService operatorService;

    @Inject
    WebsiteRepository websiteRepository;


    public void registerSimpleWeb() throws GitAPIException, IOException, URISyntaxException {
        websiteRepository.addWebsite(websiteRepository.createWebsite("simple", OperatorServiceTest.SIMPLE_WEB));
        operatorService.initServices(OperatorServiceTest.SIMPLE_WEB);
    }
}
