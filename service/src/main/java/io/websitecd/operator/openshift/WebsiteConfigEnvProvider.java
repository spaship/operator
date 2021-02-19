package io.websitecd.operator.openshift;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.controller.OperatorService;
import io.websitecd.operator.controller.WebsiteRepository;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.crd.WebsiteSpec;
import io.websitecd.operator.websiteconfig.GitWebsiteConfigService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

@ApplicationScoped
public class WebsiteConfigEnvProvider {

    private static final Logger log = Logger.getLogger(WebsiteConfigEnvProvider.class);

    @ConfigProperty(name = "website.name")
    Optional<String> websiteName;
    @ConfigProperty(name = "website.gitUrl")
    Optional<String> gitUrl;

    @ConfigProperty(name = "website.branch")
    Optional<String> branch;
    @ConfigProperty(name = "website.webhook.secret")
    Optional<String> secret;

    @ConfigProperty(name = "website.sslVerify")
    Optional<Boolean> sslVerify;

    @ConfigProperty(name = "website.config.dir")
    Optional<String> configDir;

    @ConfigProperty(name = "app.operator.provider.env.delay")
    protected long initDelay;

    @ConfigProperty(name = "website.namespace")
    protected Optional<String> namespace;

    @ConfigProperty(name = "app.operator.provider.env.enabled")
    Optional<Boolean> providerEnabled;

    @Inject
    OperatorService operatorService;

    @Inject
    Vertx vertx;

    @Inject
    WebsiteRepository websiteRepository;

    @Inject
    GitWebsiteConfigService gitWebsiteConfigService;

    void onStart(@Observes StartupEvent ev) {
        if (!providerEnabled.orElse(false)) {
            log.infof("No Git URL Defined in env variable. Skipping");
            return;
        }
        // TODO validate input values
        WebsiteSpec websiteSpec = new WebsiteSpec(gitUrl.get(), branch.orElse(null), configDir.orElse(null), sslVerify.orElse(true), secret.get());
        Website website = WebsiteRepository.createWebsite(websiteName.get(), websiteSpec, namespace.get());
        
        log.infof("Registering INIT EnvProvider with delay=%s website=%s", initDelay, websiteSpec);
        vertx.setTimer(initDelay, e -> {
            vertx.executeBlocking(future -> {
                try {
                    registerWebsite(website);
                } catch (Exception ex) {
                    future.fail(ex);
                }
                future.complete();
            }, res -> {
                if (res.succeeded()) {
                    log.infof("Initialization completed from ENV provider.");
                } else {
                    log.error("Cannot init ENV provider", res.cause());
                }
            });
        });
    }

    public void registerWebsite(Website website) throws IOException, GitAPIException, URISyntaxException {
        WebsiteConfig websiteConfig = gitWebsiteConfigService.cloneRepo(website);
        website.setConfig(websiteConfig);
        websiteRepository.addWebsite(website);
        operatorService.initNewWebsite(website);
    }

}
