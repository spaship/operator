package io.websitecd.operator.content;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.websitecd.content.git.config.GitContentUtils;
import io.websitecd.content.git.config.model.ContentConfig;
import io.websitecd.operator.Utils;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.Yaml;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ContentController {

    private static final Logger log = Logger.getLogger(ContentController.class);

    static final String CONFIG_INIT = "-content-init-";

    @Inject
    DefaultOpenShiftClient client;

    @Inject
    Vertx vertx;

    Map<String, WebClient> clients = new HashMap<>();

    @ConfigProperty(name = "quarkus.http.root-path")
    String rootPath;

    @ConfigProperty(name = "app.content.git.api.host")
    Optional<String> staticContentHost;

    @ConfigProperty(name = "app.content.git.api.port")
    int staticContentApiPort;

    @ConfigProperty(name = "app.content.git.rootcontext")
    protected String rootContext;

    @ConfigProperty(name = "app.operator.website.config.sslverify")
    boolean sslVerify;

    public String getContentHost(String env, WebsiteConfig config) {
        return staticContentHost.orElse(Utils.getWebsiteName(config) + "-content-" + env);
    }

    public void createClient(String gitUrl, String env, WebsiteConfig config) {
        String host = getContentHost(env, config);
        WebClient websiteClient = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(host)
                .setDefaultPort(staticContentApiPort)
                .setTrustAll(true));
        String clientId = gitUrl + "-" + env;
        clients.put(clientId, websiteClient);
        log.infof("Content client API created. clientId=%s host=%s port=%s", clientId, host, staticContentApiPort);
    }


    public void updateConfigs(String env, String namespace, WebsiteConfig websiteConfig) {
        ContentConfig config = GitContentUtils.createConfig(env, websiteConfig, rootContext);
        String data = new Yaml().dumpAsMap(config);
        final String configName = Utils.getWebsiteName(websiteConfig) + CONFIG_INIT + env;
        updateConfigSecret(configName, namespace, data);
    }

    public void updateConfigSecret(String name, String namespace, String secretData) {
        log.infof("Update content-init in namespace=%s, name=%s", namespace, name);

        Map<String, String> data = new HashMap<>();
        data.put("content-config-git.yaml", secretData);

        log.tracef("%s=\n%s", name, data);

        SecretBuilder config = new SecretBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(name).build())
                .withStringData(data);
        client.inNamespace(namespace).secrets().createOrReplace(config.build());
    }

    public void deploy(String env, String namespace, String websiteName, WebsiteConfig config) {
        Map<String, String> params = new HashMap<>();
        params.put("ENV", env);
        params.put("NAME", websiteName);
        params.put("GIT_SSL_NO_VERIFY", Boolean.toString(!sslVerify));

        KubernetesList result = client.templates()
                .inNamespace(namespace)
                .load(ContentController.class.getResourceAsStream("/openshift/content-template.yaml"))
                .processLocally(params);

        log.debugf("Template %s successfully processed to list with %s items",
                result.getItems().get(0).getMetadata().getName(),
                result.getItems().size());

        for (HasMetadata item : result.getItems()) {
            log.infof("Deploying kind=%s name=%s", item.getKind(), item.getMetadata().getName());
            // see https://www.javatips.net/api/fabric8-master/components/kubernetes-api/src/main/java/io/fabric8/kubernetes/api/Controller.java#
            item.getMetadata().getLabels().putAll(Utils.defaultLabels(env, config));
            if (item instanceof Service) {
                client.inNamespace(namespace).services().createOrReplace((Service) item);
            }
            if (item instanceof Deployment) {
                Deployment deployment = (Deployment) item;
                Container httpdContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);

                for (ComponentConfig component : config.getComponents()) {
                    if (!component.isKindGit()) {
                        continue;
                    }
                    if (!OperatorConfigUtils.isComponentEnabled(config, env, component.getContext())) {
                        continue;
                    }

                    final String mountPath = "/var/www/html/" + component.getContext().substring(1);  // remove starting /
                    VolumeMountBuilder vmb = new VolumeMountBuilder()
                            .withName("data")
                            .withMountPath(mountPath);

                    String subPath = GitContentUtils.getDirName(component.getContext(), rootContext);
                    if (StringUtils.isNotEmpty(component.getSpec().getDir())) {
                        subPath += component.getSpec().getDir();
                    }
                    vmb.withSubPath(subPath);
                    httpdContainer.getVolumeMounts().add(vmb.build());
                }
                log.infof("VolumeMounts=%s", httpdContainer.getVolumeMounts());

                client.inNamespace(namespace).apps().deployments().createOrReplace((Deployment) item);
            }
            if (item instanceof Route) {
                client.inNamespace(namespace).routes().createOrReplace((Route) item);
            }

        }
    }

    public void redeploy(String env, WebsiteConfig config) {
        String componentName = Utils.getWebsiteName(config) + "-content-" + env;
        String ns = config.getEnvironment(env).getNamespace();
        client.inNamespace(ns).apps().deployments().withName(componentName).rolling().restart();
        log.infof("deployment rollout name=%s", componentName);
    }

    public Uni<String> refreshComponent(WebClient webClient, String name) {
        log.infof("Refresh component name=%s", name);
        return webClient.get("/api/update/" + name).send()
                .onItem().invoke(resp -> {
                    if (resp.statusCode() != 200) {
                        throw new RuntimeException(resp.bodyAsString());
                    }
                }).map(resp -> resp.bodyAsString());
    }

    public Multi<String> listComponents(WebClient webClient) {
        log.infof("List components");

        return webClient.get("/api/list").send()
                .onItem().transformToMulti(resp -> {
                    List<String> array = resp.bodyAsJsonArray().getList();
                    return Multi.createFrom().iterable(array);
                });
    }

    public WebClient getContentApiClient(String clientId) {
        return clients.get(clientId);
    }

    public int getStaticContentApiPort() {
        return staticContentApiPort;
    }
}
