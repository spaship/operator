package io.spaship.operator.router;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.spaship.operator.QuarkusTestBase;
import io.spaship.operator.crd.Website;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;

import static io.spaship.operator.rest.WebhookTestCommon.NAMESPACE;
import static io.spaship.operator.router.RouterControllerTest.*;

@QuarkusTest
class IngressControllerTest extends QuarkusTestBase {

    @Inject
    IngressController controller;

    @BeforeEach
    protected void setupNetworking() {
        mockServer.expect()
                .post().withPath("/apis/networking.k8s.io/v1/namespaces/" + NAMESPACE + "/ingresses")
                .andReturn(200, new IngressBuilder().withMetadata(new ObjectMetaBuilder().withName("simple-test").build()).build()).always();
    }

    @Test
    void testIngress() {
        Website website = createTestWebsite(List.of(componentRoot, componentService));

        Ingress ingress = controller.updateIngress("test", website);

        Assertions.assertNotNull(ingress);

        assertPathsRequested("/apis/networking.k8s.io/v1/namespaces/spaship-examples/ingresses");
    }

}