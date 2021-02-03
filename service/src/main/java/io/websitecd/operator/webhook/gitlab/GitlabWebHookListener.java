package io.websitecd.operator.webhook.gitlab;

import io.vertx.mutiny.ext.web.client.WebClient;
import io.websitecd.operator.config.model.Environment;
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
import java.util.List;
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

    @ConfigProperty(name = "app.operator.website.config.filenames")
    String[] websiteYamlName;

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
                    operatorService.processConfig(gitUrl, true, false);
                    rollout = true;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        for (Map.Entry<String, WebsiteConfig> configEntry : websiteConfigService.getWebsites().entrySet()) {
            if (rollout && StringUtils.equals(configEntry.getKey(), gitUrl)) {
                log.debugf("component is already covered by rolling update. going to next component. ");
                continue;
            }
            WebsiteConfig config = configEntry.getValue();
            Map<String, Environment> envs = config.getEnvs();
            for (Map.Entry<String, Environment> envEntry : envs.entrySet()) {
                if (!operatorService.isEnvEnabled(envEntry.getValue())) {
                    log.debugf("Env is not enabled");
                    continue;
                }
                updateAllComponents(gitUrl, envEntry.getKey());
            }
        }
    }


    protected void updateAllComponents(String gitUrl, String env) {
        String clientId = gitUrl + "-" + env;
        WebClient contentApiClient = contentController.getContentApiClient(clientId);
        if (contentApiClient == null) {
            throw new RuntimeException("contentApiClient not defined for gitUrl=" + clientId);
        }
        log.infof("Update all components on clientId=%s ", clientId);
        contentController.listComponents(contentApiClient)
                .onItem().transform(jsonArray -> jsonArray.getList())
                .subscribe()
                .with(items -> {
                    for (Object name : items) {
                        log.debugf("Going to update component name=%s", name);
                        contentController.refreshComponent(contentApiClient, name.toString())
                                .subscribe()
                                .with(s -> log.infof("updated name=%s result=%s", name, s));
                    }
                });
    }

    public static boolean isRolloutNeeded(Event event, String... yamlNames) {
        if (event instanceof PushEvent) {
            PushEvent pushEvent = (PushEvent) event;
            for (EventCommit commit : pushEvent.getCommits()) {
                if (containsString(commit.getModified(), yamlNames)) {
                    return true;
                }
                if (containsString(commit.getAdded(), yamlNames)) {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean containsString(List<String> list, String... searchStrings) {
        if (list == null || list.size() == 0) {
            return false;
        }
        for (String s : list) {
            if (StringUtils.containsAny(s, searchStrings)) {
                return true;
            }
        }
        return false;
    }

    public boolean deploymentChanged(WebsiteConfig oldConfig, WebsiteConfig newConfig) {
        // TODO: Compare old and new config and consider if deployment has changed
        return true;
    }

}
