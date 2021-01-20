package io.websitecd.operator.webhook.gitlab;

import io.websitecd.operator.openshift.OperatorService;
import io.websitecd.operator.openshift.WebsiteConfigService;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.WebHookListener;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class GitlabEventListener implements WebHookListener {

    private static final Logger log = Logger.getLogger(GitlabEventListener.class);

    @Inject
    WebsiteConfigService websiteConfigService;

    @Inject
    OperatorService operatorService;


    @Override
    public void onPushEvent(PushEvent event) {
        log.infof("push event");
        try {
//            WebsiteConfig config = websiteConfigService.updateRepo();
//            operatorService.processConfig(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
