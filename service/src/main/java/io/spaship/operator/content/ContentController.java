package io.spaship.operator.content;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.spaship.content.git.config.GitContentUtils;
import io.spaship.content.git.config.model.ContentConfig;
import io.spaship.operator.Utils;
import io.spaship.operator.config.KubernetesUtils;
import io.spaship.operator.config.OperatorConfigUtils;
import io.spaship.operator.config.model.ComponentConfig;
import io.spaship.operator.config.model.DeploymentConfig;
import io.spaship.operator.config.model.Environment;
import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.crd.Website;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.Yaml;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@ApplicationScoped
public class ContentController {

    private static final Logger log = Logger.getLogger(ContentController.class);

    static final String CONFIG_INIT = "-content-init-";

    @Inject
    DefaultOpenShiftClient client;

    @Inject
    Vertx vertx;

    Map<String, WebClient> clients = new HashMap<>();

    @ConfigProperty(name = "app.content.git.api.host")
    Optional<String> contentApiHost;

    @ConfigProperty(name = "app.content.git.api.port")
    int staticContentApiPort;

    // Content Image Spec
    @ConfigProperty(name = "app.operator.image.init.name")
    Optional<String> imageInitName;
    @ConfigProperty(name = "app.operator.image.init.version")
    Optional<String> imageInitVersion;
    @ConfigProperty(name = "app.operator.image.httpd.name")
    Optional<String> imageHttpdName;
    @ConfigProperty(name = "app.operator.image.httpd.version")
    Optional<String> imageHttpdVersion;
    @ConfigProperty(name = "app.operator.image.api.name")
    Optional<String> imageApiName;
    @ConfigProperty(name = "app.operator.image.api.version")
    Optional<String> imageApiVersion;

    @ConfigProperty(name = "app.content.git.rootcontext")
    protected String rootContext;

    @ConfigProperty(name = "app.operator.content.envs")
    Optional<String> contentEnvsStr;

    Map<String, Environment> contentEnvs;

    void startup(@Observes StartupEvent event) {
        log.infof("ContentController init. contentApiHost=%s staticContentApiPort=%s rootContext=%s " +
                        "imageInitName=%s imageInitVersion=%s imageHttpdName=%s imageHttpdVersion=%s imageApiName=%s imageApiVersion=%s ",
                contentApiHost.orElse("N/A"), staticContentApiPort, rootContext,
                imageInitName.orElse("N/A"), imageInitVersion.orElse("N/A"),
                imageHttpdName.orElse("N/A"), imageHttpdVersion.orElse("N/A"),
                imageApiName.orElse("N/A"), imageApiVersion.orElse("N/A")
        );
        log.infof("contentEnvs=%s", contentEnvsStr.orElse("N/A"));
        try {
            contentEnvs = OperatorConfigUtils.getContentEnvsJson(contentEnvsStr.orElse(null));
        } catch (IOException e) {
            log.error("Invalid `APP_OPERATOR_CONTENT_ENVS` variable. value=" + contentEnvsStr.orElse("N/A"));
            throw new ConfigurationException(e);
        }
    }

    public String getContentHost(String env, Website config) {
        if (contentApiHost.isPresent()) {
            return contentApiHost.get();
        }
        String serviceName = Utils.getWebsiteName(config) + "-content-" + env;
        String namespace = config.getMetadata().getNamespace();
        return serviceName + "." + namespace + ".svc.cluster.local";
    }

    protected static String getClientId(Website website, String env) {
        return website.getMetadata().getNamespace() + "_" + website.getMetadata().getName() + "_" + env;
    }

    public void createClient(String env, Website config, String host, Integer port) {
        String clientId = getClientId(config, env);
        if (clients.containsKey(clientId)) {
            log.debugf("Client already exists. skipping. clientId=%s", clientId);
            return;
        }
        if (StringUtils.isEmpty(host)) {
            host = getContentHost(env, config);
        }
        if (port == null) {
            port = staticContentApiPort;
        }
        WebClient websiteClient = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(host)
                .setDefaultPort(port)
                .setTrustAll(true));
        clients.put(clientId, websiteClient);
        log.infof("Content client API created. clientId=%s host=%s port=%s", clientId, host, port);
    }

    public void removeClient(String env, Website config) {
        clients.remove(getClientId(config, env));
    }

    public void updateConfigs(String env, String namespace, Website website) {
        ContentConfig config = GitContentUtils.createConfig(env, website.getConfig(), rootContext);
        String data = new Yaml().dumpAsMap(config);
        final String configName = getInitConfigName(website, env);
        Map<String, String> labels = Utils.defaultLabels(env, website);
        updateConfigMap(configName, namespace, data, labels);
    }

    public String getInitConfigName(Website website, String env) {
        return Utils.getWebsiteName(website) + CONFIG_INIT + env;
    }

    public void deleteConfigs(String env, String namespace, Website website) {
        final String configName = getInitConfigName(website, env);
        client.inNamespace(namespace).configMaps().withName(configName).delete();
    }

    public void updateConfigMap(String name, String namespace, String secretData, Map<String, String> labels) {
        log.infof("Update content-init in namespace=%s, name=%s", namespace, name);

        Map<String, String> data = new HashMap<>();
        data.put("content-config-git.yaml", secretData);

        log.tracef("%s=\n%s", name, data);

        ConfigMapBuilder config = new ConfigMapBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(name).withLabels(labels).build())
                .withData(data);
        client.inNamespace(namespace).configMaps().createOrReplace(config.build());
    }

    public void deploy(String env, String namespace, String websiteName, Website website) {
        WebsiteConfig config = website.getConfig();

        Map<String, String> params = new HashMap<>();
        params.put("ENV", env);
        params.put("NAME", websiteName);
        params.put("GIT_SSL_NO_VERIFY", Boolean.toString(!website.getSpec().getSslVerify()));
        imageInitName.ifPresent(s -> params.put("IMAGE_INIT", s));
        imageHttpdName.ifPresent(s -> params.put("IMAGE_HTTPD", s));
        imageApiName.ifPresent(s -> params.put("IMAGE_API", s));
        imageInitVersion.ifPresent(s -> params.put("IMAGE_INIT_VERSION", s));
        imageHttpdVersion.ifPresent(s -> params.put("IMAGE_HTTPD_VERSION", s));
        imageApiVersion.ifPresent(s -> params.put("IMAGE_API_VERSION", s));

        KubernetesList result = processTemplate(namespace, params);

        Map<String, String> defaultLabels = Utils.defaultLabels(env, website);

        for (HasMetadata item : result.getItems()) {
            log.infof("Deploying kind=%s name=%s", item.getKind(), item.getMetadata().getName());
            // see https://www.javatips.net/api/fabric8-master/components/kubernetes-api/src/main/java/io/fabric8/kubernetes/api/Controller.java#
            item.getMetadata().getLabels().putAll(defaultLabels);
            if (item instanceof Service) {
                client.inNamespace(namespace).services().createOrReplace((Service) item);
            }
            if (item instanceof Deployment) {
                Deployment deployment = (Deployment) item;

                // override operator's defaults
                DeploymentConfig operatorOverride = getOperatorDeploymentOverride(contentEnvs, env, website.getSpec().getPreviews());
                if (operatorOverride != null) {
                    log.infof("Applying operator deployment override");
                    log.tracef("operator-override=%s", operatorOverride);
                    deployment = overrideDeployment(deployment, operatorOverride);
                }
                // override website's defaults
                DeploymentConfig websiteOverride = config.getEnvironment(env).getDeployment();
                if (websiteOverride != null) {
                    log.info("Applying website deployment override");
                    log.tracef("website-override=%s", websiteOverride);
                    deployment = overrideDeployment(deployment, websiteOverride);
                }

                Container httpdContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);

                config.getEnabledGitComponents(env)
                        .map(this::createVolumeMountBuilder)
                        .forEach(vmb -> httpdContainer.getVolumeMounts().add(vmb.build()));

                log.tracef("deployment=%s", deployment);

                client.inNamespace(namespace).apps().deployments().createOrReplace(deployment);
            }
            if (item instanceof Route) {
                client.inNamespace(namespace).routes().createOrReplace((Route) item);
            }

        }
    }

    protected VolumeMountBuilder createVolumeMountBuilder(ComponentConfig component) {
        final String mountPath = "/var/www/html/" + component.getComponentName();
        VolumeMountBuilder vmb = new VolumeMountBuilder()
                .withName("data")
                .withMountPath(mountPath);

        String subPath = GitContentUtils.getDirName(component.getContext(), rootContext);
        if (StringUtils.isNotEmpty(component.getSpec().getDir())) {
            subPath += component.getSpec().getDir();
        }
        vmb.withSubPath(subPath);
        return vmb;
    }

    public String getComponentDirName(ComponentConfig component) {
        return GitContentUtils.getDirName(component.getContext(), rootContext);
    }

    protected KubernetesList processTemplate(String namespace, Map<String, String> params) {
        KubernetesList result = client.templates()
                .inNamespace(namespace)
                .load(ContentController.class.getResourceAsStream("/openshift/content-template.yaml"))
                .processLocally(params);

        log.debugf("Template %s successfully processed to list with %s items",
                result.getItems().get(0).getMetadata().getName(),
                result.getItems().size());
        return result;
    }

    public DeploymentConfig getOperatorDeploymentOverride(Map<String, Environment> contentEnvs, String env, boolean isPreview) {
        if (contentEnvs == null || contentEnvs.isEmpty()) return null;

        String contentEnvName = env;
        if (isPreview) contentEnvName = "preview";

        if (!contentEnvs.containsKey(contentEnvName)) {
            // exact match not found. try to find via regexp
            for (String envRegexp : contentEnvs.keySet()) {
                if (Pattern.matches(envRegexp, contentEnvName)) {
                    contentEnvName = envRegexp;
                    break;
                }
            }
        }

        if (!contentEnvs.containsKey(contentEnvName)) return null;

        return contentEnvs.get(contentEnvName).getDeployment();
    }

    public Deployment overrideDeployment(Deployment deployment, DeploymentConfig config) {
        if (config == null) return deployment;
        DeploymentSpec spec = deployment.getSpec();
        if (config.getReplicas() != null) spec.setReplicas(config.getReplicas());

        Container initContainer = spec.getTemplate().getSpec().getInitContainers().get(0);
        Container httpdContainer = spec.getTemplate().getSpec().getContainers().get(0);
        Container apiContainer = spec.getTemplate().getSpec().getContainers().get(1);

        KubernetesUtils.overrideContainer(initContainer, config.getInit());
        KubernetesUtils.overrideContainer(httpdContainer, config.getHttpd());
        KubernetesUtils.overrideContainer(apiContainer, config.getApi());

        return deployment;
    }

    public void redeploy(String env, Website website) {
        String componentName = Utils.getWebsiteName(website) + "-content-" + env;
        String ns = website.getMetadata().getNamespace();
        client.inNamespace(ns).apps().deployments().withName(componentName).rolling().restart();
        log.infof("deployment rollout name=%s", componentName);
    }

    public void deleteDeployment(String env, String namespace, String websiteName) {
        Map<String, String> params = new HashMap<>();
        params.put("ENV", env);
        params.put("NAME", websiteName);

        KubernetesList result = processTemplate(namespace, params);

        for (HasMetadata item : result.getItems()) {
            log.infof("Deleting deployment kind=%s name=%s", item.getKind(), item.getMetadata().getName());
            if (item instanceof Service) {
                client.inNamespace(namespace).services().withName(item.getMetadata().getName()).delete();
            }
            if (item instanceof Deployment) {
                client.inNamespace(namespace).apps().deployments().withName(item.getMetadata().getName()).delete();
            }
        }
    }

    public Future<UpdatedComponent> refreshComponent(Website website, String env, String name) {
        String componentDesc = String.format("websiteId=%s env=%s name=%s", website.getId(), env, name);
        log.infof("Update components on %s", componentDesc);
        String clientId = getClientId(website, env);
        WebClient webClient = clients.get(clientId);

        Promise<UpdatedComponent> promise = Promise.promise();
        if (webClient == null) {
            ServiceUnavailableException exception = new ServiceUnavailableException("Client not available clientId=" + clientId);
            log.error("Content client not found", exception);
            promise.tryFail(exception);
            return promise.future();
        }

        webClient.get("/api/update/" + name)
                .expect(ResponsePredicate.SC_OK)
                .send(ar -> {
                    log.tracef("update result=%s", ar);
                    if (ar.succeeded()) {
                        UpdatedComponent result = new UpdatedComponent(name,
                                ar.result().bodyAsString(),
                                website.getMetadata().getNamespace(),
                                website.getMetadata().getName(),
                                env);
                        promise.tryComplete(result);
                    } else {
                        String message = String.format("Cannot update content on %s clientId=%s", componentDesc, clientId);
                        log.error(message, ar.cause());
                        promise.tryFail(new InternalServerErrorException(message));
                    }
                });
        return promise.future();
    }

    public Future<JsonObject> componentInfo(Website website, String env, String name) {
        String componentDesc = String.format("websiteId=%s env=%s name=%s", website.getId(), env, name);
        log.infof("Get Info components on %s", componentDesc);
        String clientId = getClientId(website, env);
        WebClient webClient = clients.get(clientId);

        Promise<JsonObject> promise = Promise.promise();
        if (webClient == null) {
            ServiceUnavailableException exception = new ServiceUnavailableException("Client not available clientId=" + clientId);
            log.error("Content client not found", exception);
            promise.tryFail(exception);
            return promise.future();
        }

        webClient.get("/api/info/" + name)
                .send(ar -> {
                    if (ar.result().statusCode() == 404) {
                        promise.tryFail(new NotFoundException("Component not found. name=" + name));
                        return;
                    }
                    if (ar.result().statusCode() != 200) {
                        log.error(String.format("Error getting info on %s clientId=%s", componentDesc, clientId), ar.cause());
                        promise.tryFail(ar.cause());
                    } else {
                        JsonObject result = ar.result().bodyAsJsonObject();
                        log.tracef("info result=%s", result);
                        promise.tryComplete(result);
                    }
                });
        return promise.future();
    }

    public Future<JsonArray> listComponents(WebClient webClient) {
        log.infof("List components");

        Promise<JsonArray> promise = Promise.promise();
        webClient.get("/api/list").send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            if (result.result().statusCode() != 200) {
                promise.tryFail(result.result().bodyAsString());
            } else {
                promise.tryComplete(result.result().bodyAsJsonArray());
            }
        });
        return promise.future();
    }

    public int getStaticContentApiPort() {
        return staticContentApiPort;
    }
}
