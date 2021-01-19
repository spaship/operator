package io.websitecd.operator.content;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
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
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class ContentController {

    private static final Logger log = Logger.getLogger(ContentController.class);

    static final String CONFIG_INIT = "-content-init-";
    static final String CONFIG_HTTPD = "-content-httpd-";

    @Inject
    DefaultOpenShiftClient client;

    @Inject
    Vertx vertx;

    WebClient staticContentClient;

    @ConfigProperty(name = "quarkus.http.root-path")
    String rootPath;

    @ConfigProperty(name = "app.content.git.api.host")
    String staticContentHost;

    @ConfigProperty(name = "app.content.git.api.port")
    int staticContentApiPort;

    @ConfigProperty(name = "app.content.git.rootcontext")
    protected String rootContext;

    void onStart(@Observes StartupEvent ev) {
        this.staticContentClient = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(staticContentHost)
                .setDefaultPort(staticContentApiPort)
                .setTrustAll(true));
        log.infof("Static content client created host=%s port=%s", staticContentHost, staticContentApiPort);
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
                String dir = c.getContext();
                if (StringUtils.equals("/", c.getContext())) {
                    continue;
                }
                config.append("Alias " + dir + " /var/www/components" + dir);
            }
        }
        return config;
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


    public void deploy(String env, String namespace, String websiteName) {
        final Template serverUploadedTemplate = client.templates()
                .inNamespace(namespace)
                .load(ContentController.class.getResourceAsStream("/openshift/core-staticcontent.yaml"))
                .createOrReplace();
        String templateName = serverUploadedTemplate.getMetadata().getName();
        log.infof("Template %s successfully created on server, namespace=%s", serverUploadedTemplate.getMetadata().getName(), namespace);

        Map<String, String> params = new HashMap<>();
        params.put("ENV", env);
        params.put("NAME", websiteName);

        KubernetesList result = client.templates()
                .inNamespace(namespace).withName(templateName)
                .process(params);

        log.debugf("Template %s successfully processed to list with %s items",
                result.getItems().get(0).getMetadata().getName(),
                result.getItems().size());

        for (HasMetadata item : result.getItems()) {
            // see https://www.javatips.net/api/fabric8-master/components/kubernetes-api/src/main/java/io/fabric8/kubernetes/api/Controller.java#
            item.getMetadata().getLabels().putAll(Utils.defaultLabels(env));
            log.infof("Deploying kind=%s name=%s", item.getKind(), item.getMetadata().getName());
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

    public void redeploy(String env, String namespace) {
        String name = "static-" + env;
        client.inNamespace(namespace).apps().deployments().withName(name).rolling().restart();
        log.infof("deployment rollout name=%s", name);
    }

    public Uni<JsonObject> refreshComponent(String name) {
        log.infof("Refresh component name=%s", name);
        return staticContentClient.get("/_staticcontent/api/update/" + name).send().map(resp -> {
            if (resp.statusCode() == 200) {
                return resp.bodyAsJsonObject();
            } else {
                return new JsonObject()
                        .put("code", resp.statusCode())
                        .put("message", resp.bodyAsString());
            }
        });
    }

    public Uni<JsonObject> listComponents() {
        log.infof("List components");
        return staticContentClient.get("/_staticcontent/api/list").send().map(resp -> {
            if (resp.statusCode() == 200) {
                return resp.bodyAsJsonObject();
            } else {
                return new JsonObject()
                        .put("code", resp.statusCode())
                        .put("message", resp.bodyAsString());
            }
        });
    }

}
