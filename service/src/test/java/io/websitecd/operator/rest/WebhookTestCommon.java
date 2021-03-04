package io.websitecd.operator.rest;

import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.websitecd.operator.ContentApiMock;
import io.websitecd.operator.QuarkusTestBase;
import io.websitecd.operator.controller.OperatorService;
import io.websitecd.operator.controller.WebsiteRepository;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.crd.WebsiteSpec;
import io.websitecd.operator.openshift.OperatorServiceTest;
import io.websitecd.operator.openshift.WebsiteConfigEnvProvider;
import io.websitecd.operator.websiteconfig.GitWebsiteConfigService;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;

public class WebhookTestCommon extends QuarkusTestBase {

    public static final String GIT_EXAMPLES_URL = "https://github.com/websitecd/websitecd-examples.git";
    public static final String GIT_EXAMPLES_BRANCH = "integration-test";
    public static final String GIT_EXAMPLES_CONFIG_SIMPLE = "websites/01-simple";
    public static final String GIT_EXAMPLES_CONFIG_ADVANCED = "websites/02-advanced";
    public static final String SECRET_SIMPLE = "testsecret_simple";
    public static final String SECRET_ADVANCED = "testsecret_advanced";
    public static final String SECRET_SIMPLE_SIGN = DigestUtils.sha256Hex(SECRET_SIMPLE);
    public static final String SECRET_ADVANCED_SIGN = DigestUtils.sha256Hex(SECRET_ADVANCED);

    public static final WebsiteSpec SIMPLE_WEB = new WebsiteSpec(GIT_EXAMPLES_URL, GIT_EXAMPLES_BRANCH, GIT_EXAMPLES_CONFIG_SIMPLE, true, SECRET_SIMPLE);
    public static final WebsiteSpec ADVANCED_WEB = new WebsiteSpec(GIT_EXAMPLES_URL, GIT_EXAMPLES_BRANCH, GIT_EXAMPLES_CONFIG_ADVANCED, true, SECRET_ADVANCED);

    public static final String NAMESPACE = "websitecd-examples";

    public static final Website SIMPLE_WEBSITE = WebsiteRepository.createWebsite("simple", SIMPLE_WEB, NAMESPACE);
    public static final Website ADVANCED_WEBSITE = WebsiteRepository.createWebsite("advanced", ADVANCED_WEB, NAMESPACE);

    @Inject
    OperatorService operatorService;
    @Inject
    WebsiteRepository websiteRepository;
    @Inject
    GitWebsiteConfigService gitWebsiteConfigService;
    @Inject
    WebsiteConfigEnvProvider websiteConfigEnvProvider;

    protected static Vertx vertx;
    protected static ContentApiMock apiMock;

    @BeforeAll
    static void beforeAll() {
        vertx = Vertx.vertx();
        apiMock = new ContentApiMock(8001);
        vertx.deployVerticle(apiMock);
    }

    @BeforeEach
    void beforeEach() {
        apiMock.reset();
    }
    @AfterEach
    void afterEach() {
        apiMock.reset();
    }

    @AfterAll
    static void afterAll() {
        vertx.close();
    }

    public void registerWeb(Website website) throws IOException, GitAPIException, URISyntaxException {
        websiteRepository.reset();
        websiteConfigEnvProvider.registerWebsite(website);
    }

    public void registerSimpleWeb() throws GitAPIException, IOException, URISyntaxException {
        registerWeb(SIMPLE_WEBSITE);
    }

    public void registerAdvancedWeb() throws GitAPIException, IOException, URISyntaxException {
        registerWeb(ADVANCED_WEBSITE);
    }

    public String getGitlabEventBody(String gitUrl, String branch) {
        return new JsonObject()
                .put("ref", "refs/heads/" + branch)
                .put("repository", new JsonObject().put("git_http_url", gitUrl))
                .toString();
    }

    public RequestSpecification givenSimpleGitlabWebhookRequest() {
        return given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_SIMPLE);
    }

}
