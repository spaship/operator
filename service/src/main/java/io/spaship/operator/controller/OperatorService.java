package io.spaship.operator.controller;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.openshift.api.model.Route;
import io.quarkus.runtime.StartupEvent;
import io.spaship.content.git.config.GitContentUtils;
import io.spaship.operator.Utils;
import io.spaship.operator.config.model.ComponentConfig;
import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.content.ContentController;
import io.spaship.operator.content.UpdatedComponent;
import io.spaship.operator.crd.Website;
import io.spaship.operator.crd.WebsiteStatus;
import io.spaship.operator.router.IngressController;
import io.spaship.operator.router.RouterController;
import io.spaship.operator.websiteconfig.GitWebsiteConfigService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.spaship.operator.config.matcher.ComponentKindMatcher.ComponentGitMatcher;

@ApplicationScoped
public class OperatorService {

    private static final Logger log = Logger.getLogger(OperatorService.class);

    @Inject
    ContentController contentController;

    @Inject
    RouterController routerController;

    @Inject
    IngressController ingressController;

    @Inject
    WebsiteRepository websiteRepository;

    @Inject
    GitWebsiteConfigService gitWebsiteConfigService;

    @ConfigProperty(name = "app.content.git.rootcontext")
    protected String rootContext;

    @Inject
    Vertx vertx;

    // eager start
    void startup(@Observes StartupEvent event) {
    }

    public WebsiteStatus initNewWebsite(Website website) {
        return initInfrastructure(website, false);
    }

    public WebsiteStatus initInfrastructure(Website website, boolean redeploy) {
        Set<String> enabledEnvs = website.getEnabledEnvs();
        log.infof("Init infrastructure for websiteId=%s, enabledEnvs=%s", website.getId(), enabledEnvs);

        RuntimeException exception = null;
        WebsiteStatus status = website.getStatus();
        if (status == null) {
            status = new WebsiteStatus();
        }
        for (String env : enabledEnvs) {
            try {
                log.debugf("Processing env=%s", env);

                setupCoreServices(env, website);

                String apiHost = null;
                Integer port = null;
                if (ingressController.isEnabled()) {
                    Ingress ingress = ingressController.updateIngress(env, website);
                    if (ingress != null) {
                        String contentHost = ingress.getSpec().getRules().get(0).getHost();
                        status.addEnvHost(env, contentHost);
                    }
                } else if (routerController.isEnabled()) {
                    List<Route> contentRoutes = routerController.updateWebsiteRoutes(env, website);
                    if (contentRoutes != null && contentRoutes.size() > 0) {
                        String contentHost = contentRoutes.get(0).getSpec().getHost();
                        status.addEnvHost(env, contentHost);
                    }
                    Route apiRoute = routerController.updateApiRoute(env, website);
                    apiHost = apiRoute.getSpec().getHost();
                    port = 80;
                } else {
                    log.infof("No routing created");
                }
                contentController.createClient(env, website, apiHost, port);

                if (redeploy) {
                    contentController.redeploy(env, website);
                }
            } catch (RuntimeException ex) {
                log.error("Error processing env=" + env, ex);
                exception = ex;
                // continue with processing other environments and throw exception after loop ends
            }
        }
        if (exception != null) {
            throw exception;
        }
        log.debugf("Infrastructure initialized. status=%s", status);
        websiteRepository.addWebsite(website);
        return status;
    }

    private void setupCoreServices(String env, Website website) {
        String namespace = website.getMetadata().getNamespace();
        final String websiteName = Utils.getWebsiteName(website);
        log.infof("Create core services websiteName=%s env=%s namespace=%s", websiteName, env, namespace);
        contentController.updateConfigs(env, namespace, website);
        contentController.deploy(env, namespace, websiteName, website);
    }

    public void deleteInfrastructure(Website website) {
        log.infof("Delete infrastructure for websiteId=%s", website.getId());

        String namespace = website.getMetadata().getNamespace();
        final String websiteName = Utils.getWebsiteName(website);

        for (String env : website.getEnabledEnvs()) {
            contentController.deleteDeployment(env, namespace, websiteName);
            contentController.deleteConfigs(env, namespace, website);

            if (ingressController.isEnabled()) {
                ingressController.deleteIngress(env, website);
            } else if (routerController.isEnabled()) {
                routerController.deleteWebsiteRoutes(env, website);
            } else {
                log.infof("No routing deleted");
            }

            contentController.removeClient(env, website);
        }
    }

    public List<Future> updateRelatedComponents(String gitUrl, String ref, Set<String> updatedWebsites) {
        List<Future> updates = new ArrayList<>();
        for (Website website : websiteRepository.getWebsites().values()) {
            if (updatedWebsites != null && updatedWebsites.contains(website.getId())) {
                continue;
            }
            // secret token is not checked
            for (ComponentConfig component : website.getConfig().getComponents()) {
                if (!ComponentGitMatcher.test(component) || !StringUtils.equals(gitUrl, component.getSpec().getUrl())) {
                    continue;
                }
                log.tracef("Component with same gitUrl found. context=%s", component.getContext());
                List<Future<UpdatedComponent>> componentsUpdates = website.getEnabledEnvs().stream()
                        .filter(env -> {
                            String componentRef = GitContentUtils.getRef(website.getConfig(), env, component.getContext());
                            return ref.endsWith(componentRef);
                        })
                        .map(env -> contentController.refreshComponent(website, env, GitContentUtils.getDirName(component.getContext(), rootContext)))
                        .collect(Collectors.toList());
                updates.addAll(componentsUpdates);
            }
        }
        return updates;
    }

    /**
     * Rollout website
     *
     * @param website
     * @param forceRollout if true new and old config is not checked
     * @return
     * @throws GitAPIException
     * @throws IOException
     */
    public boolean rolloutWebsiteNonBlocking(Website website, boolean forceRollout) throws GitAPIException, IOException {
        String websiteId = website.getId();

        WebsiteConfig newConfig = gitWebsiteConfigService.updateRepo(website);
        if (!forceRollout) {
            Website oldWebsite = websiteRepository.getWebsite(website.getId());
            if (newConfig.equals(oldWebsite.getConfig())) {
                log.infof("Configs are same for websiteId=%s. No rollout performed", websiteId);
                return false;
            }
        }

        vertx.executeBlocking(future -> {
            log.infof("Rollout websiteId=%s", websiteId);
            try {
                website.setConfig(newConfig);

                initInfrastructure(website, true);
                future.complete();
            } catch (Exception e) {
                future.fail(e.getMessage());
            }
        }, res -> {
            if (res.succeeded()) {
                log.infof("Website updated websiteId=%s", websiteId);
            } else {
                log.error("Cannot update website, websiteId=" + websiteId, res.cause());
            }
        });
        return true;
    }

}
