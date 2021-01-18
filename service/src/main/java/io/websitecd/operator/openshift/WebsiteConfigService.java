package io.websitecd.operator.openshift;

import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Optional;

@ApplicationScoped
public class WebsiteConfigService {

    private static final Logger log = Logger.getLogger(WebsiteConfigService.class);

    @ConfigProperty(name = "app.operator.website.url")
    String gitUrl;

    String workDir = System.getProperty("user.dir");

    @ConfigProperty(name = "app.operator.website.config.dir")
    Optional<String> configDir;

    @ConfigProperty(name = "app.operator.website.config.filename")
    String configFilename;

    @ConfigProperty(name = "app.operator.website.config.sslverify")
    Boolean sslVerify;

    WebsiteConfig config;

    public void cloneRepo() throws GitAPIException, IOException, URISyntaxException {
        log.info("Initializing website config");
        File gitDir = getGitDir();
        if (!gitDir.exists()) {
            Git git = Git.init().setDirectory(gitDir).call();
            git.remoteAdd().setName("origin").setUri(new URIish(gitUrl)).call();

            StoredConfig config = git.getRepository().getConfig();
            config.setBoolean("http", null, "sslVerify", sslVerify);
            config.save();

            git.pull().call();

            String lastCommitMessage = git.log().call().iterator().next().getShortMessage();
            log.infof("Website config cloned to dir=%s commit_message='%s'", gitDir, lastCommitMessage);
            git.close();
        } else {
            log.infof("Website config already cloned. skipping dir=%s", gitDir);
        }
        try (InputStream is = new FileInputStream(getWebsiteConfigPath(gitDir.getAbsolutePath()))) {
            config = OperatorConfigUtils.loadYaml(is);
        }
    }

    public void reload() throws GitAPIException, IOException {
        File gitDir = getGitDir();
        PullResult pullResult = Git.open(gitDir).pull().call();
        if (!pullResult.isSuccessful()) {
            throw new RuntimeException("Cannot pull repo. result=" + pullResult);
        }
        log.infof("Website config pulled in dir=%s commit_message='%s'", gitDir, pullResult.getFetchResult().getMessages());

        try (InputStream is = new FileInputStream(getWebsiteConfigPath(gitDir.getAbsolutePath()))) {
            config = OperatorConfigUtils.loadYaml(is);
        }
    }

    public WebsiteConfig getConfig() {
        return config;
    }

    public String getWebsiteConfigPath(String baseDir) {
        if (configDir.isEmpty()) {
            return baseDir + "/" + configFilename;
        } else {
            return baseDir + "/" + configDir + "/" + configFilename;
        }
    }

    public File getGitDir() {
        return new File(workDir + "/website.git");
    }


}