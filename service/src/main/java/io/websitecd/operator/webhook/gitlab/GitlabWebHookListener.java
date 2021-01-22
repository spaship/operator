package io.websitecd.operator.webhook.gitlab;

import io.vertx.mutiny.ext.web.client.WebClient;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.openshift.OperatorService;
import io.websitecd.operator.openshift.WebsiteConfigService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.gitlab4j.api.webhook.Event;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.WebHookListener;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

@ApplicationScoped
public class GitlabWebHookListener implements WebHookListener {

    private static final Logger log = Logger.getLogger(GitlabWebHookListener.class);


    @Inject
    WebsiteConfigService websiteConfigService;

    @Inject
    OperatorService operatorService;

    @Inject
    ContentController contentController;

    @ConfigProperty(name = "app.operator.website.config.filename")
    String websiteYamlName;


    @Override
    public void onPushEvent(PushEvent pushEvent) {
        String gitUrl = pushEvent.getRepository().getGit_http_url();
        handleEvent(gitUrl, pushEvent);
    }

    public void handleEvent(String gitUrl, Event event) {
        boolean isWebsite = websiteConfigService.isKnownWebsite(gitUrl);
        boolean isComponent = websiteConfigService.isKnownComponent(gitUrl);
        if (!isWebsite || !isComponent) {
            log.infof("git url unknown. ignoring. gitUrl=%s", gitUrl);
            return;
        }

        boolean rollout = false;
        if (isWebsite && isRolloutNeeded(event, websiteYamlName)) {
            WebsiteConfig oldConfig = websiteConfigService.getConfig(gitUrl);
            try {
                WebsiteConfig newConfig = websiteConfigService.updateRepo(gitUrl);
                if (deploymentChanged(oldConfig, newConfig)) {
                    operatorService.processConfig(gitUrl, true);
                    rollout = true;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        for (Map.Entry<String, WebsiteConfig> config : websiteConfigService.getWebsites().entrySet()) {
            if (rollout && StringUtils.equals(config.getKey(), gitUrl)) {
                log.debugf("website is already covered by rolling update. going to next component");
                continue;
            }

            for (ComponentConfig component : config.getValue().getComponents()) {
                if (component.isKindGit()) {
                    if (StringUtils.equals(gitUrl, component.getSpec().getUrl())) {
                        // TODO: Lookup the env based on branch/tag
                        String clientId = gitUrl + "-" + "dev";
                        WebClient contentApiClient = contentController.getContentApiClient(clientId);
                        if (contentApiClient == null) {
                            throw new RuntimeException("contentApiClient not defined for gitUrl=" + clientId);
                        }
                        log.infof("Update content on clientId=%s ", clientId);
                        contentController.listComponents(contentApiClient)
                                .onItem().transform(jsonArray -> jsonArray.getList())
                                .subscribe()
                                .with(items -> {
                                    for (Object name : items) {
                                        contentController.refreshComponent(contentApiClient, name.toString())
                                                .subscribe()
                                                .with(s -> log.infof("updated name=%s result=%s", name, s));
                                    }
                                });
                    }
                }
            }
        }
    }

    public static boolean isRolloutNeeded(Event event, String yamlName) {
        if (event instanceof PushEvent) {
            PushEvent pushEvent = (PushEvent) event;
            for (EventCommit commit : pushEvent.getCommits()) {
                if (commit.getModified().contains(yamlName)) {
                    return true;
                }
                if (commit.getAdded().contains(yamlName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean deploymentChanged(WebsiteConfig oldConfig, WebsiteConfig newConfig) {
        // TODO: Compare old and new config and consider if deployment has changed
        return true;
    }

}
