package io.websitecd.operator.websiteconfig;

import io.websitecd.operator.config.OperatorConfigUtils;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class GitWebsiteConfigService {

    private static final Logger log = Logger.getLogger(GitWebsiteConfigService.class);

    String workDir = System.getProperty("user.dir");

    @ConfigProperty(name = "app.operator.website.config.filenames")
    String[] configFilename;

    Map<String, GitInfo> repos = new HashMap<>();

    public WebsiteConfig cloneRepo(Website website) throws GitAPIException, IOException, URISyntaxException {
        WebsiteSpec websiteSpec = website.getSpec();
        log.infof("Initializing website config spec=%s", websiteSpec);
        final String gitUrl = websiteSpec.getGitUrl();
        final String branch = websiteSpec.getBranch();
        File gitDir = new File(getGitDirName(workDir, website.getId()));

        Git git = Git.init().setDirectory(gitDir).call();
        git.remoteAdd().setName("origin").setUri(new URIish(gitUrl)).call();
        if (!gitDir.exists()) {
            StoredConfig config = git.getRepository().getConfig();
            config.setBoolean("http", null, "sslVerify", websiteSpec.getSslVerify());
            config.save();

            git.pull().setRemoteBranchName(branch).call();

            String lastCommitMessage = git.log().call().iterator().next().getShortMessage();
            log.infof("Website config cloned to dir=%s commit_message='%s'", gitDir, lastCommitMessage);
        } else {
            log.debugf("Website config already cloned. performing git pull dir=%s", gitDir);
            git.pull().setRemoteBranchName(branch).call();

            String lastCommitMessage = git.log().call().iterator().next().getShortMessage();
            log.infof("Website config pulled to dir=%s commit_message='%s'", gitDir, lastCommitMessage);
        }
        git.close();
        GitInfo gitInfo = new GitInfo(website.getSpec().getBranch(), gitDir.getAbsolutePath(), website.getSpec().getDir());
        repos.put(website.getId(), gitInfo);

        return getConfig(website);
    }

    public void deleteRepo(Website website) throws IOException {
        final String id = website.getId();
        GitInfo gitInfo = repos.get(id);
        File gitDir = new File(gitInfo.getDir());
        Files.walk(gitDir.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        repos.remove(id);
        log.infof("Git dir deleted gitDir=%s", gitDir.getAbsolutePath());
    }

    public WebsiteConfig updateRepo(Website website) throws GitAPIException, IOException {
        GitInfo gitInfo = repos.get(website.getId());
        File gitDir = new File(gitInfo.getDir());
        PullResult pullResult = Git.open(gitDir).pull().setRemoteBranchName(gitInfo.getBranch()).call();
        if (!pullResult.isSuccessful()) {
            throw new RuntimeException("Cannot pull repo. result=" + pullResult);
        }
        FetchResult fetchResult = pullResult.getFetchResult();
        log.infof("Website config pulled in dir=%s commit_message='%s'", gitDir, fetchResult.getMessages());

        return getConfig(website);
    }

    protected void registerNewGitRepo(Website website, String gitDirPath) {
    }

    public WebsiteConfig getConfig(Website website) throws IOException {
        GitInfo gitInfo = repos.get(website.getId());
        String gitUrl = website.getSpec().getGitUrl();
        File gitDir = new File(gitInfo.getDir());

        File configFile = getWebsiteConfigPath(gitDir, gitInfo.getConfigDir());
        WebsiteConfig config = loadConfig(configFile);
        int applied = OperatorConfigUtils.applyDefaultGirUrl(config, gitUrl);
        if (applied > 0) {
            log.infof("git url set for %s components", applied);
        }
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

    public static String getGitDirName(String workDir, String websiteId) {
        return workDir + "/git-website_" + websiteId.replace(".", "_").replace("/", "").replace(":", "_");
    }

}