package io.websitecd.operator.openshift;

import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class WebsiteConfigService {

    private static final Logger log = Logger.getLogger(WebsiteConfigService.class);


    String workDir = System.getProperty("user.dir");

    @ConfigProperty(name = "app.operator.website.config.dir")
    Optional<String> configDir;

    @ConfigProperty(name = "app.operator.website.config.filename")
    String configFilename;

    @ConfigProperty(name = "app.operator.website.config.sslverify")
    Boolean sslVerify;

    Map<String, WebsiteConfig> websites = new HashMap<>();

    public WebsiteConfig cloneRepo(String gitUrl, String branch) throws GitAPIException, IOException, URISyntaxException {
        log.info("Initializing website config");
        File gitDir = new File(getGitDirName(workDir, gitUrl));
        if (!gitDir.exists()) {
            Git git = Git.init().setDirectory(gitDir).call();
            git.remoteAdd().setName("origin").setUri(new URIish(gitUrl)).call();

            StoredConfig config = git.getRepository().getConfig();
            config.setBoolean("http", null, "sslVerify", sslVerify);
            config.save();

            git.pull().setRemoteBranchName(branch).call();

            String lastCommitMessage = git.log().call().iterator().next().getShortMessage();
            log.infof("Website config cloned to dir=%s commit_message='%s'", gitDir, lastCommitMessage);
            git.close();
        } else {
            log.infof("Website config already cloned. skipping dir=%s", gitDir);
        }
        WebsiteConfig config;
        try (InputStream is = new FileInputStream(getWebsiteConfigPath(gitDir.getAbsolutePath()))) {
            config = OperatorConfigUtils.loadYaml(is);
        }
        log.infof("Registering config under gitUrl=%s", gitUrl);
        websites.put(gitUrl, config);
        return config;
    }

    public WebsiteConfig updateRepo(String gitUrl) throws GitAPIException, IOException {
        File gitDir = new File(getGitDirName(workDir, gitUrl));
        PullResult pullResult = Git.open(gitDir).pull().call();
        if (!pullResult.isSuccessful()) {
            throw new RuntimeException("Cannot pull repo. result=" + pullResult);
        }
        FetchResult fetchResult = pullResult.getFetchResult();
        log.infof("Website config pulled in dir=%s commit_message='%s'", gitDir, fetchResult.getMessages());

        WebsiteConfig config;
        try (InputStream is = new FileInputStream(getWebsiteConfigPath(gitDir.getAbsolutePath()))) {
            config = OperatorConfigUtils.loadYaml(is);
        }
        log.infof("Updating config under gitUrl=%s", gitUrl);
        websites.put(gitUrl, config);
        return config;
    }

    public String getWebsiteConfigPath(String baseDir) {
        if (configDir.isEmpty()) {
            return baseDir + "/" + configFilename;
        } else {
            return baseDir + "/" + configDir.get() + "/" + configFilename;
        }
    }

    public static String getGitDirName(String workDir, String gitUrl) {
        return workDir + "/" + gitUrl.replace(".", "_").replace("/", "").replace(":", "_");
    }

    public boolean isKnownWebsite(String gitUrl) {
        return websites.containsKey(gitUrl);
    }

    public boolean isKnownComponent(String gitUrl) {
        for (WebsiteConfig config : websites.values()) {
            for (ComponentConfig component : config.getComponents()) {
                if (component.isKindGit()) {
                    if (StringUtils.equals(gitUrl, component.getSpec().getUrl())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public WebsiteConfig getConfig(String gitUrl) {
        return websites.get(gitUrl);
    }

    public Map<String, WebsiteConfig> getWebsites() {
        return websites;
    }
}