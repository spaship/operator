package io.websitecd.operator.openshift;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.websitecd.operator.Utils;
import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.controller.WebsiteRepository;
import io.websitecd.operator.crd.WebsiteSpec;
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
    WebsiteConfigService websiteConfigService;

    @Inject
    RouterController routerController;

    @Inject
    IngressController ingressController;

    @Inject
    DefaultOpenShiftClient client;

    @Inject
    WebsiteRepository websiteRepository;


    public void initServices(WebsiteSpec websiteSpec) throws IOException, GitAPIException, URISyntaxException {
        initServices(websiteSpec, null);
    }

    public void initServices(WebsiteSpec websiteSpec, String namespace) throws IOException, GitAPIException, URISyntaxException {
        log.infof("Init service. openshift_url=%s", client.getOpenshiftUrl());

        String gitUrl = websiteSpec.getGitUrl();
        try {
            websiteConfigService.cloneRepo(websiteSpec);

            processConfig(gitUrl, false, true, namespace);
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

    public void processConfig(String gitUrl, boolean redeploy, boolean createClients, String namespace) {
        WebsiteConfig config = websiteConfigService.getConfig(gitUrl);
        Map<String, Environment> envs = config.getEnvs();
        for (Map.Entry<String, Environment> envEntry : envs.entrySet()) {
            String env = envEntry.getKey();
            if (!isEnvEnabled(envEntry.getValue(), namespace)) {
                log.infof("environment ignored env=%s namespace=%s", env, namespace);
                continue;
            }
            log.infof("Processing env=%s", env);
            if (createClients) {
                contentController.createClient(gitUrl, env, config);
            }

            // TODO: Create it in working thread or async
            setupCoreServices(env, config);
            if (redeploy) {
                contentController.redeploy(env, config);
            }
        }
    }

    public void setupCoreServices(String env, WebsiteConfig config) {
        String namespace = config.getEnvironment(env).getNamespace();
        final String websiteName = Utils.getWebsiteName(config);
        log.infof("Create core services websiteName=%s env=%s namespace=%s", websiteName, env, namespace);
        contentController.updateConfigs(env, namespace, config);
        contentController.deploy(env, namespace, websiteName, config);

        if (StringUtils.equals(routerMode, "ingress")) {
            ingressController.updateIngress(env, namespace, config);
        } else if (StringUtils.equals(routerMode, "openshift")) {
            routerController.updateWebsiteRoutes(env, namespace, config);
        } else {
            log.infof("No routing created");
        }
    }

}
