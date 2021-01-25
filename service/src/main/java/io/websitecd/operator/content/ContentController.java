package io.websitecd.operator.content;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.websitecd.content.git.config.GitContentUtils;
import io.websitecd.content.git.config.model.ContentConfig;
import io.websitecd.operator.Utils;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.ComponentSpec;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.Yaml;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ContentController {

    private static final Logger log = Logger.getLogger(ContentController.class);

    static final String CONFIG_INIT = "-content-init-";
    static final String CONFIG_HTTPD = "-content-httpd-";

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

    public StringBuffer createAliases(String targetEnv, WebsiteConfig websiteConfig) {
        StringBuffer config = new StringBuffer();
        if (!OperatorConfigUtils.isEnvEnabled(websiteConfig, targetEnv)) {
            return config;
        }
        for (ComponentConfig c : websiteConfig.getComponents()) {
            ComponentSpec spec = c.getSpec();
            if (!OperatorConfigUtils.isEnvIncluded(spec.getEnvs(), targetEnv)) {
                continue;
            }

            if (c.getKind().equals("git")) {
                final String context = c.getContext();
                if (StringUtils.equals("/", context)) {
                    continue;
                }
                String path = getAliasPath("/var/www/components", c);
                config.append("Alias " + context + " " + path);
            }
        }
        return config;
    }

    public static String getAliasPath(String rootPath, ComponentConfig c) {
        final String context = c.getContext();
        final String specDir = c.getSpec().getDir();
        StringBuilder path = new StringBuilder(rootPath);
        path.append(context);
        if (StringUtils.isNotEmpty(specDir) && !StringUtils.equals("/", specDir)) {
            path.append(specDir);
        }
        String result = path.toString();
        result = result.replace("//", "/");
        // MUST end with / otherwise alias doesn't work correctly
        if (!result.endsWith("/")) {
            result += "/";
        }
        return result;
    }


    public void updateConfigs(String env, String namespace, WebsiteConfig websiteConfig) {
        ContentConfig config = GitContentUtils.createConfig(env, websiteConfig, rootContext);
        String data = new Yaml().dumpAsMap(config);
        final String configName = Utils.getWebsiteName(websiteConfig) + CONFIG_INIT + env;
        updateConfigSecret(configName, namespace, data);

        String aliases = createAliases(env, websiteConfig).toString();
        final String httpdName = Utils.getWebsiteName(websiteConfig) + CONFIG_HTTPD + env;
        updateConfigHttpdSecret(httpdName, namespace, aliases);
    }

    public void updateConfigSecret(String name, String namespace, String secretData) {
        log.infof("Update content-init in namespace=%s, name=%s", namespace, name);

        Map<String, String> data = new HashMap<>();
        data.put("content-config-git.yaml", secretData);

        log.debugf("%s=\n%s", name, data);

        SecretBuilder config = new SecretBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(name).build())
                .withStringData(data);
        client.inNamespace(namespace).secrets().createOrReplace(config.build());
    }

    public void updateConfigHttpdSecret(String name, String namespace, String aliasesData) {
        log.infof("Update content-httpd in namespace=%s, name=%s", namespace, name);

        Map<String, String> dataAlias = new HashMap<>();
        dataAlias.put("aliases.conf", aliasesData);

        log.debugf("%s=\n%s", name, dataAlias);


        SecretBuilder configAlias = new SecretBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(name).build())
                .withStringData(dataAlias);
        client.inNamespace(namespace).secrets().createOrReplace(configAlias.build());
    }


    public void deploy(String env, String namespace, String websiteName, String contentRootSubpath) {
//        final Template serverUploadedTemplate = client.templates()
//                .inNamespace(namespace)
//                .load(ContentController.class.getResourceAsStream("/openshift/content-template.yaml"))
//                .createOrReplace();
//        String templateName = serverUploadedTemplate.getMetadata().getName();
//        log.infof("Template %s successfully created on server, namespace=%s", serverUploadedTemplate.getMetadata().getName(), namespace);

        Map<String, String> params = new HashMap<>();
        params.put("ENV", env);
        params.put("NAME", websiteName);
        if (StringUtils.isNotBlank(contentRootSubpath)) {
            params.put("CONTENT_ROOT_SUBPATH", contentRootSubpath);
        }

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
            item.getMetadata().getLabels().putAll(Utils.defaultLabels(env));
            if (item instanceof Service) {
                client.inNamespace(namespace).services().createOrReplace((Service) item);
            }
            if (item instanceof Deployment) {
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

    public Uni<JsonArray> listComponents(WebClient webClient) {
        log.infof("List components");

        return webClient.get("/api/list").send()
                .onItem().invoke(resp -> {
                    if (resp.statusCode() != 200) {
                        throw new RuntimeException(resp.bodyAsString());
                    }
                })
                .map(resp -> resp.bodyAsJsonArray());
    }

    public WebClient getContentApiClient(String clientId) {
        return clients.get(clientId);
    }

}
