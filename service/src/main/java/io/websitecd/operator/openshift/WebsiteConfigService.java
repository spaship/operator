package io.websitecd.operator.openshift;

import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.crd.WebsiteSpec;
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

@ApplicationScoped
public class WebsiteConfigService {

    private static final Logger log = Logger.getLogger(WebsiteConfigService.class);


    String workDir = System.getProperty("user.dir");

    @ConfigProperty(name = "app.operator.website.config.filenames")
    String[] configFilename;

    Map<String, WebsiteConfig> websites = new HashMap<>();

    Map<String, GitInfo> repos = new HashMap<>();

    public WebsiteConfig cloneRepo(Website website) throws GitAPIException, IOException, URISyntaxException {
        WebsiteSpec websiteSpec = website.getSpec();
        log.infof("Initializing website config spec=%s", websiteSpec);
        String gitUrl = websiteSpec.getGitUrl();
        String branch = websiteSpec.getBranch();
        File gitDir = new File(getGitDirName(workDir, gitUrl));
        if (!gitDir.exists()) {
            Git git = Git.init().setDirectory(gitDir).call();
            git.remoteAdd().setName("origin").setUri(new URIish(gitUrl)).call();

            StoredConfig config = git.getRepository().getConfig();
            config.setBoolean("http", null, "sslVerify", websiteSpec.getSslVerify());
            config.save();

            git.pull().setRemoteBranchName(branch).call();

            String lastCommitMessage = git.log().call().iterator().next().getShortMessage();
            log.infof("Website config cloned to dir=%s commit_message='%s'", gitDir, lastCommitMessage);
            git.close();

        } else {
            log.infof("Website config already cloned. skipping dir=%s", gitDir);
        }
        repos.put(gitUrl, new GitInfo(branch, gitDir.getAbsolutePath(), websiteSpec.getDir()));

        File configFile = getWebsiteConfigPath(gitDir, websiteSpec.getDir());
        WebsiteConfig config = loadConfig(configFile);
        int applied = OperatorConfigUtils.applyDefaultGirUrl(config, gitUrl);
        if (applied > 0) {
            log.infof("git url set for %s components", applied);
        }
        registerWebsiteConfig(website, config);
        return config;
    }

    private void registerWebsiteConfig(Website website, WebsiteConfig config) {
        String id = createWebsiteConfigId(website);
        log.infof("Registering config under id=%s", id);
        websites.put(id, config);
    }

    public String createWebsiteConfigId(Website website) {
        return website.getMetadata().getNamespace() + "-" + website.getMetadata().getName() + "-" + website.getSpec().getGitUrl();
    }

    public WebsiteConfig updateRepo(Website website) throws GitAPIException, IOException {
        String gitUrl = website.getSpec().getGitUrl();
        GitInfo gitInfo = repos.get(gitUrl);
        File gitDir = new File(gitInfo.getDir());
        PullResult pullResult = Git.open(gitDir).pull().setRemoteBranchName(gitInfo.getBranch()).call();
        if (!pullResult.isSuccessful()) {
            throw new RuntimeException("Cannot pull repo. result=" + pullResult);
        }
        FetchResult fetchResult = pullResult.getFetchResult();
        log.infof("Website config pulled in dir=%s commit_message='%s'", gitDir, fetchResult.getMessages());

        File configFile = getWebsiteConfigPath(gitDir, gitInfo.getConfigDir());
        WebsiteConfig config = loadConfig(configFile);
        int applied = OperatorConfigUtils.applyDefaultGirUrl(config, gitUrl);
        if (applied > 0) {
            log.infof("git url set for %s components", applied);
        }
        registerWebsiteConfig(website, config);
        return config;
    }

    public WebsiteConfig loadConfig(File configFile) throws IOException {
        if (configFile == null || !configFile.exists()) {
            throw new IOException("Website config file not exists path=" + configFile);
        }
        try (InputStream is = new FileInputStream(configFile)) {
            return OperatorConfigUtils.loadYaml(is);
        }
    }

    public File getWebsiteConfigPath(File baseDir, String configDir) {
        if (StringUtils.isEmpty(configDir)) {
            return getWebsiteFile(baseDir);
        } else {
            return getWebsiteFile(new File(baseDir.getAbsolutePath(), configDir));
        }
    }

    private File getWebsiteFile(File websiteDir) {
        for (String filename : configFilename) {
            File file = new File(websiteDir.getAbsolutePath(), filename);
            if (file.exists()) {
                log.infof("website config found. path=%s", file.getAbsolutePath());
                return file;
            }
        }
        return null;
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

    public WebsiteConfig getConfig(Website website) {
        return websites.get(createWebsiteConfigId(website));
    }

    public Map<String, WebsiteConfig> getWebsites() {
        return websites;
    }

    public static class GitInfo {
        String branch;
        String dir;
        String configDir;

        public GitInfo(String branch, String dir, String configDir) {
            this.branch = branch;
            this.dir = dir;
            this.configDir = configDir;
        }

        public String getBranch() {
            return branch;
        }

        public String getDir() {
            return dir;
        }

        public String getConfigDir() {
            return configDir;
        }
    }

}