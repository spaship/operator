package io.spaship.operator.router;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.spaship.operator.QuarkusTestBase;
import io.spaship.operator.config.model.ComponentConfig;
import io.spaship.operator.config.model.ComponentSpec;
import io.spaship.operator.config.model.Environment;
import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.crd.Website;
import io.spaship.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class RouterControllerTest extends QuarkusTestBase {

    public final static ComponentConfig componentRoot = new ComponentConfig("/", ComponentConfig.KIND_GIT, ComponentSpec.createGitSpec("url", null, "main"));
    public final static ComponentConfig componentSearch = new ComponentConfig("/search", ComponentConfig.KIND_GIT, ComponentSpec.createGitSpec("url", null, "main"));
    public final static ComponentConfig componentService = new ComponentConfig("/api", ComponentConfig.KIND_SERVICE, ComponentSpec.createServiceSpec("api", 8080));

    @Inject
    RouterController controller;

    @BeforeEach
    protected void setupNetworking() {
        mockServer.expect()
                .post().withPath("/apis/route.openshift.io/v1/namespaces/spaship-examples/routes")
                .andReturn(200, new RouteBuilder().build()).always();
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
    void testHostDomainNull() {
        assertNull(RouterController.getHost(Optional.empty(), null, null, null));
    }

    @Test
    void testHostDomain() {
        assertEquals("web-env-nm.test.info", RouterController.getHost(Optional.of("test.info"), "web", "env", "nm"));
    }

    @Test
    void testWebsiteRoutes() {
        List<Route> routes = controller.updateWebsiteRoutes("test", createTestWebsite(List.of(componentRoot, componentSearch, componentService)));
        assertEquals(2, routes.size());
        assertPathsRequested("/apis/route.openshift.io/v1/namespaces/spaship-examples/routes", "/apis/route.openshift.io/v1/namespaces/spaship-examples/routes");
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

    @Test
    void testApiRoute() {
        Route route = controller.updateApiRoute("test", createTestWebsite(List.of(componentRoot, componentSearch, componentService)));
        Assertions.assertNotNull(route);
        assertPathsRequested("/apis/route.openshift.io/v1/namespaces/spaship-examples/routes");
    }

    @Test
    void testApiRouteHost() {
        Route route = controller.createApiRoute("test", createTestWebsite(List.of(componentRoot, componentSearch, componentService)));
        assertEquals("simple-test-api-spaship-examples.test.info", route.getSpec().getHost());
    }
}