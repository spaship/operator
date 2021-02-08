package io.websitecd.operator.openshift;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.websitecd.operator.Utils;
import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.controller.WebsiteRepository;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.router.IngressController;
import io.websitecd.operator.router.RouterController;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

@ApplicationScoped
public class OperatorService {

    private static final Logger log = Logger.getLogger(OperatorService.class);


    @ConfigProperty(name = "app.operator.router.mode")
    String routerMode;

    @Inject
    ContentController contentController;

    @Inject
    GitWebsiteConfigService gitWebsiteConfigService;

    @Inject
    RouterController routerController;

    @Inject
    IngressController ingressController;

    @Inject
    DefaultOpenShiftClient client;

    @Inject
    WebsiteRepository websiteRepository;


    public void initServices(Website website) throws IOException, GitAPIException, URISyntaxException {
        log.infof("Init service. openshift_url=%s", client.getOpenshiftUrl());

        String gitUrl = website.getSpec().getGitUrl();
        try {
            gitWebsiteConfigService.cloneRepo(website);

            initInfrastructure(website, false, true);
        } catch (Exception e) {
            log.error("Cannot init core services for gitUrl=" + gitUrl, e);
            throw e;
        }
    }

    public boolean isEnvEnabled(Environment env, String namespace) {
        if (namespace == null) {
            return true;
        }
        return env.getNamespace().equals(namespace);
    }

    public void initNewWebsite(Website website) {
        initInfrastructure(website, false, true);
    }

    public void initInfrastructure(Website website, boolean redeploy, boolean createClients) {
        log.infof("Init infrastructure for website=%s", website);

        WebsiteConfig config = website.getConfig();
        String namespace = website.getMetadata().getNamespace();

        Map<String, Environment> envs = config.getEnvs();
        for (Map.Entry<String, Environment> envEntry : envs.entrySet()) {
            String env = envEntry.getKey();
            if (!isEnvEnabled(envEntry.getValue(), namespace)) {
                log.infof("environment ignored env=%s namespace=%s", env, namespace);
                continue;
            }
            log.infof("Processing env=%s", env);
            if (createClients) {
                contentController.createClient(env, website);
            }

            // TODO: Create it in working thread or async
            setupCoreServices(env, website);
            if (redeploy) {
                contentController.redeploy(env, website);
            }
        }
    }

    public void setupCoreServices(String env, Website website) {
        WebsiteConfig config = website.getConfig();
        String namespace = website.getMetadata().getNamespace();
        final String websiteName = Utils.getWebsiteName(website);
        log.infof("Create core services websiteName=%s env=%s namespace=%s", websiteName, env, namespace);
        contentController.updateConfigs(env, namespace, website);
        contentController.deploy(env, namespace, websiteName, config);

        if (StringUtils.equals(routerMode, "ingress")) {
            ingressController.updateIngress(env, website);
        } else if (StringUtils.equals(routerMode, "openshift")) {
            routerController.updateWebsiteRoutes(env, website);
        } else {
            log.infof("No routing created");
        }
    }

}
