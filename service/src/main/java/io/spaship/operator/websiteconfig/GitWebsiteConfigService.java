package io.spaship.operator.websiteconfig;

import io.quarkus.runtime.StartupEvent;
import io.spaship.operator.config.OperatorConfigUtils;
import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.crd.Website;
import io.spaship.operator.crd.WebsiteSpec;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class GitWebsiteConfigService {

    private static final Logger log = Logger.getLogger(GitWebsiteConfigService.class);

    String workDir = System.getProperty("user.dir");

    @ConfigProperty(name = "app.operator.website.config.filenames")
    Set<String> configFilenames;

    Map<String, GitInfo> repos = new HashMap<>();

    void onStart(@Observes StartupEvent ev) {
        log.infof("WebsiteConfig Service init. configFilename=%s", configFilenames);
    }

    public WebsiteConfig cloneRepo(Website website, boolean updateIfExists) throws GitAPIException, IOException {
        WebsiteSpec websiteSpec = website.getSpec();
        final String gitUrl = websiteSpec.getGitUrl();
        File gitDir = new File(getGitDirName(workDir, website.getId()));
        boolean gitDirExists = gitDir.exists();
        log.infof("Initializing website config repo in dir=%s gitDirExists=%s websiteSpec=%s", gitDir, gitDirExists, websiteSpec);

        Git git = null;
        if (!gitDirExists) {
            CloneCommand cloneCommand = Git.cloneRepository().setURI(gitUrl).setDirectory(gitDir);
            if (!websiteSpec.getSslVerify()) {
                log.debug("ssl verification disabled");
                cloneCommand.setCredentialsProvider(new GitSSLIgnoreCredentialsProvider());
            }
            final String branch = websiteSpec.getBranch();
            if (StringUtils.isNotEmpty(branch)) {
                cloneCommand.setBranch(branch);
            }
            git = cloneCommand.call();
        } else if (updateIfExists) {
            log.debug("Just git pull");
            git = Git.init().setDirectory(gitDir).call();
            git.pull().call();
        }

        if (git != null) {
            String lastCommitMessage = git.log().call().iterator().next().getShortMessage();
            git.close();

            log.infof("Website repo fetched to dir=%s dir_exists=%s commit_message='%s'", gitDir, gitDirExists, lastCommitMessage);
        }

        GitInfo gitInfo = new GitInfo(website.getSpec().getBranch(), gitDir.getAbsolutePath(), website.getSpec().getDir());
        repos.put(website.getId(), gitInfo);

        return getConfigFromGitRepo(website);
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
        try (Git git = Git.open(gitDir)) {
            PullResult pullResult = git.pull().call();
            if (!pullResult.isSuccessful()) {
                throw new RuntimeException("Cannot pull repo. result=" + pullResult);
            }
            FetchResult fetchResult = pullResult.getFetchResult();
            log.infof("Website config pulled in dir=%s commit_message='%s'", gitDir, fetchResult.getMessages());

            return getConfigFromGitRepo(website);
        }
    }

    private WebsiteConfig getConfigFromGitRepo(Website website) throws IOException {
        GitInfo gitInfo = repos.get(website.getId());
        String gitUrl = website.getSpec().getGitUrl();
        File gitDir = new File(gitInfo.getDir());

        File configFile = getWebsiteConfigPath(gitDir, gitInfo.getConfigDir());
        WebsiteConfig config = loadConfig(configFile);
        int applied = OperatorConfigUtils.applyDefaultGirUrl(config, gitUrl);
        if (applied > 0) {
            log.debugf("git url set for %s components", applied);
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
        for (String filename : configFilenames) {
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