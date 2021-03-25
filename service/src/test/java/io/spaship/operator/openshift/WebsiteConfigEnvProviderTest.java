package io.spaship.operator.openshift;

import io.quarkus.test.junit.QuarkusTest;
import io.spaship.operator.QuarkusTestBase;
import io.spaship.operator.crd.Website;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Optional;

@QuarkusTest
class WebsiteConfigEnvProviderTest extends QuarkusTestBase {

    @Inject
    WebsiteConfigEnvProvider envProvider;

    @BeforeEach
    void setDefaultValues() {
        envProvider.setGitUrl(Optional.of("https://github.com/spaship/spaship-examples.git"));
        envProvider.setSecret(Optional.of("TOKENSIMPLE"));
        envProvider.setConfigDir(Optional.of("websites/02-advanced"));
        envProvider.setWebsiteName(Optional.of("simple"));
        envProvider.setNamespace(Optional.of("spaship-examples"));
        // To test SSL ignore feature
        envProvider.setSslVerify(Optional.of(false));
    }

    @Test
    void start() throws Exception {
        Website website = envProvider.createWebsiteFromEnv();
        envProvider.start(0, website);
        assertPathsRequested(expectedRegisterWebRequests(2));
    }

    @Test
    void missingGitUrl() {
        envProvider.setGitUrl(Optional.empty());
        Assertions.assertThrows(WebsiteConfigEnvException.class, envProvider::createWebsiteFromEnv);
    }
    @Test
    void missingSecret() {
        envProvider.setSecret(Optional.empty());
        Assertions.assertThrows(WebsiteConfigEnvException.class, envProvider::createWebsiteFromEnv);
    }
    @Test
    void missingWebsiteName() {
        envProvider.setWebsiteName(Optional.empty());
        Assertions.assertThrows(WebsiteConfigEnvException.class, envProvider::createWebsiteFromEnv);
    }
    @Test
    void missingNamespace() {
        envProvider.setNamespace(Optional.empty());
        Assertions.assertThrows(WebsiteConfigEnvException.class, envProvider::createWebsiteFromEnv);
    }
}