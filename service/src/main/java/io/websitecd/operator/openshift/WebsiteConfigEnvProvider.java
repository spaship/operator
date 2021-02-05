package io.websitecd.operator.openshift;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class WebsiteConfigEnvProvider {

    private static final Logger log = Logger.getLogger(WebsiteConfigEnvProvider.class);

    @ConfigProperty(name = "website.gitUrl")
    Optional<String> gitUrl;

    @ConfigProperty(name = "website.branch")
    Optional<String> branch;

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

    void onStart(@Observes StartupEvent ev) {
        if (!providerEnabled.orElse(false)) {
            log.infof("No Git URL Defined in env variable. Skipping");
            return;
        }
        log.infof("Registering INIT EnvProvider with delay=%s", initDelay);
        vertx.setTimer(initDelay, e -> {
            vertx.executeBlocking(future -> {
                try {
                    operatorService.initServices(gitUrl.get(), branch.get(), namespace.get());
                } catch (Exception ex) {
                    future.fail(ex);
                }
                future.complete();
            }, res -> log.infof("Initialization completed from ENV provider. success=%s", !res.failed()));
        });
    }

}
