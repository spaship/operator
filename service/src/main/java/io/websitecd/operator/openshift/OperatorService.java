package io.websitecd.operator.openshift;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.*;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.websitecd.operator.Utils;
import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.router.RouterController;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class OperatorService {

    private static final Logger log = Logger.getLogger(OperatorService.class);

    @ConfigProperty(name = "app.operator.namespace")
    protected Optional<String> namespace;

    @Inject
    ContentController contentController;

    @Inject
    WebsiteConfigService websiteConfigService;

    @Inject
    RouterController routerController;

    @Inject
    DefaultOpenShiftClient client;

    void onStart(@Observes StartupEvent ev) throws GitAPIException, IOException, URISyntaxException {
        log.infof("Openshift client_url=%s", client.getOpenshiftUrl());

        websiteConfigService.cloneRepo();
        WebsiteConfig config = websiteConfigService.getConfig();

        Map<String, Environment> envs = config.getEnvs();
        for (Map.Entry<String, Environment> envEntry : envs.entrySet()) {
            if (!namespace.isEmpty() && !envEntry.getValue().getNamespace().equals(namespace.get())) {
                log.infof("namespace ignored name=%s", namespace);
                continue;
            }
            // ? create namespace ???
            setupCoreServices(envEntry.getKey(), config);
        }

    }

//    public void createNamespaces(String prefix, List<String> envs) {
//        for (String env : envs) {
//            String name = getNameSpaceName(prefix, env);
//            Namespace ns = new NamespaceBuilder().withNewMetadata().withName(name).addToLabels("app", nameSpaceLabelValue).endMetadata().build();
//            client.namespaces().withName(name).createOrReplace(ns);
//            log.infof("Namespace created. name=%s", name);
//        }
//    }

    public void setupCoreServices(String env, WebsiteConfig config) throws MalformedURLException {
        String namespace = config.getEnvironment(env).getNamespace();
        final String websiteName = Utils.getWebsiteName(config);
        log.infof("Create core services websiteName=%s env=%s namespace=%s", websiteName, env, namespace);
        contentController.updateConfigs(env, namespace, config);
        contentController.deploy(env, namespace, websiteName);

        routerController.updateWebsiteRoutes(env, namespace, config);
    }

    public void updateServiceAccount(String namespace) {
        log.infof("Update service-account namespace=%s", namespace);
        ServiceAccountBuilder saBuilder = new ServiceAccountBuilder()
                .withMetadata(new ObjectMetaBuilder().withName("core-controller").build());
        client.inNamespace(namespace).serviceAccounts().createOrReplace(saBuilder.build());
        RoleBinding roleBinding = generateRoleBinding(namespace, namespace);
        client.inNamespace(namespace).rbac().roleBindings().createOrReplace(roleBinding);
    }

    public RoleBinding generateRoleBinding(String namespace, String watchedNamespace) {
        Subject ks = new SubjectBuilder()
                .withKind("ServiceAccount")
                .withName("core-controller")
                .withNamespace(namespace)
                .build();

        RoleRef roleRef = new RoleRefBuilder()
                .withName("core-controller")
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("ClusterRole")
                .build();

        RoleBinding rb = new RoleBindingBuilder()
                .withNewMetadata()
                .withName("core-controller")
                .withNamespace(watchedNamespace)
//                .withOwnerReferences(createOwnerReference())
//                .withLabels(labels.toMap())
                .endMetadata()
                .withRoleRef(roleRef)
                .withSubjects(ks)
                .build();

        return rb;
    }

    public void redeploy(String env, WebsiteConfig config) throws MalformedURLException {
        log.infof("Redeploying website config, env=%s", env);
        String namespace = config.getEnvironment(env).getNamespace();
        contentController.updateConfigs(env, namespace, config);
        contentController.redeploy(env, namespace);
        // TODO: Wait till deployment is ready

        routerController.updateWebsiteRoutes(env, namespace, config);
    }
}
