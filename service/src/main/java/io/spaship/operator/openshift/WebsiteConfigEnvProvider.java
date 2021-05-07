package io.spaship.operator.openshift;

import io.quarkus.runtime.StartupEvent;
import io.spaship.operator.controller.OperatorService;
import io.spaship.operator.controller.WebsiteRepository;
import io.spaship.operator.crd.Website;
import io.spaship.operator.crd.WebsiteEnvs;
import io.spaship.operator.crd.WebsiteSpec;
import io.vertx.core.Vertx;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
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
    @ConfigProperty(name = "website.previews")
    Optional<Boolean> previews;

    @ConfigProperty(name = "website.config.dir")
    Optional<String> configDir;

    @ConfigProperty(name = "website.envs.included")
    Optional<List<String>> websiteEnvIncluded;
    @ConfigProperty(name = "website.envs.excluded")
    Optional<List<String>> websiteEnvExcluded;

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

    private boolean ready = false;

    void onStart(@Observes StartupEvent ev) throws Exception {
        log.infof("WebsiteConfigEnvProvider enabled=%s", providerEnabled.orElse(false));
        if (!providerEnabled.orElse(false)) {
            ready = true;
            return;
        }
        Website website = createWebsiteFromEnv();
        log.debugf("Website from envs=%s", website);
        start(initDelay, website);
    }

    protected Website createWebsiteFromEnv() throws WebsiteConfigEnvException {
        if (gitUrl.isEmpty()) throw new WebsiteConfigEnvException("gitUrl is missing");
        if (secret.isEmpty()) throw new WebsiteConfigEnvException("secret is missing");
        if (websiteName.isEmpty()) throw new WebsiteConfigEnvException("websiteName is missing");
        if (namespace.isEmpty()) throw new WebsiteConfigEnvException("namespace is missing");

        WebsiteSpec websiteSpec = new WebsiteSpec(gitUrl.get(), branch.orElse(null), configDir.orElse(null), sslVerify.orElse(true), secret.get());
        websiteSpec.setPreviews(previews.orElse(false));
        WebsiteEnvs envs = new WebsiteEnvs();
        websiteEnvIncluded.ifPresent(envs::setIncluded);
        websiteEnvExcluded.ifPresent(envs::setExcluded);
        websiteSpec.setEnvs(envs);
        return WebsiteRepository.createWebsite(websiteName.get(), websiteSpec, namespace.get());
    }

    protected void start(long delay, Website website) throws GitAPIException, IOException, URISyntaxException {
        log.infof("Registering INIT EnvProvider with delay=%s website=%s", initDelay, website.getSpec());
        if (delay > 0) {
            vertx.setTimer(delay, e -> vertx.executeBlocking(future -> {
                try {
                    registerWebsite(website, true);
                    future.complete();
                } catch (Exception ex) {
                    future.fail(ex);
                }
            }, res -> {
                if (res.failed()) {
                    log.error("Cannot init ENV provider", res.cause());
                }
            }));
        } else {
            try {
                registerWebsite(website, true);
            } catch (Exception ex) {
                log.error("Cannot init ENV provider", ex);
                throw ex;
            }
        }
    }

    private void registerWebsite(Website website, boolean updateIfExists) throws IOException, GitAPIException {
        operatorService.deployNewWebsite(website, updateIfExists, false);
        log.infof("Initialization completed from ENV provider.");
        ready = true;
    }


    public void setWebsiteName(Optional<String> websiteName) {
        this.websiteName = websiteName;
    }

    public void setGitUrl(Optional<String> gitUrl) {
        this.gitUrl = gitUrl;
    }

    public void setBranch(Optional<String> branch) {
        this.branch = branch;
    }

    public void setSecret(Optional<String> secret) {
        this.secret = secret;
    }

    public void setSslVerify(Optional<Boolean> sslVerify) {
        this.sslVerify = sslVerify;
    }

    public void setConfigDir(Optional<String> configDir) {
        this.configDir = configDir;
    }

    public void setInitDelay(long initDelay) {
        this.initDelay = initDelay;
    }

    public void setNamespace(Optional<String> namespace) {
        this.namespace = namespace;
    }

    public boolean isReady() {
        return ready;
    }
}
