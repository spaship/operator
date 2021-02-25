package io.websitecd.operator.router;

import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.openshift.api.model.Route;
import io.quarkus.test.junit.QuarkusTest;
import io.websitecd.operator.QuarkusTestBase;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.ComponentSpec;
import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class RouterControllerTest extends QuarkusTestBase {

    public static ComponentConfig componentRoot = new ComponentConfig("/", ComponentConfig.KIND_GIT, ComponentSpec.createGitSpec("url", null, "main"));
    public static ComponentConfig componentSearch = new ComponentConfig("/search", ComponentConfig.KIND_GIT, ComponentSpec.createGitSpec("url", null, "main"));
    public static ComponentConfig componentService = new ComponentConfig("/api", ComponentConfig.KIND_SERVICE, ComponentSpec.createServiceSpec("api", 8080));

    @Inject
    RouterController controller;

    @BeforeEach
    protected void setupNetworking() {
        mockServer.expect()
                .post().withPath("/apis/route.openshift.io/v1/namespaces/websitecd-examples/routes")
                .andReturn(200, new IngressBuilder().build()).always();
    }

    public static WebsiteConfig createTestWebsiteConfig(List<ComponentConfig> components) {
        WebsiteConfig config = new WebsiteConfig();
        config.setComponents(components);
        config.setEnvs(Map.of("test", new Environment()));
        return config;
    }

    public static Website createTestWebsite(List<ComponentConfig> components) {
        Website website = WebhookTestCommon.SIMPLE_WEBSITE;
        website.setConfig(createTestWebsiteConfig(components));
        return website;
    }

    @Test
    void testWebsiteRoutes() {
        List<Route> routes = controller.updateWebsiteRoutes("test", createTestWebsite(List.of(componentRoot, componentSearch, componentService)));
        Assertions.assertEquals(2, routes.size());
    }

    @Test
    void testRootOnly() {
        List<ComponentConfig> components = controller.getGitComponents(createTestWebsiteConfig(List.of(componentRoot, componentSearch, componentService)), "test").collect(Collectors.toList());
        assertEquals(1, components.size());
        assertEquals("/", components.get(0).getContext());
    }

    @Test
    void testGitComponents() {
        List<ComponentConfig> components = controller.getGitComponents(createTestWebsiteConfig(List.of(componentSearch, componentService)), "test").collect(Collectors.toList());
        assertEquals(1, components.size());
        assertEquals("/search", components.get(0).getContext());
    }

    @Test
    void testServiceComponents() {
        List<ComponentConfig> components = controller.getServiceComponents(createTestWebsiteConfig(List.of(componentSearch, componentService)), "test").collect(Collectors.toList());
        assertEquals(1, components.size());
        assertEquals("/api", components.get(0).getContext());
    }

}