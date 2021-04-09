package io.spaship.operator.rest.website.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.core.json.JsonObject;

public class ComponentDetail extends Component {
    String timestamp;
    String gitUrl;
    String lastCommitMessage;
    String lastCommitAuthor;

    public static ComponentDetail createFrom(JsonObject info) {
        ComponentDetail data = new ComponentDetail();
        data.gitUrl = info.getString("url");
        data.setRef(info.getString("branch"));
        JsonObject lastCommit = info.getJsonObject("lastCommit");
        if (lastCommit != null) {
            data.timestamp = lastCommit.getString("timestamp");
            data.lastCommitMessage = lastCommit.getString("message");
            data.lastCommitAuthor = lastCommit.getString("author");
        }
        return data;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getLastCommitMessage() {
        return lastCommitMessage;
    }

    public void setLastCommitMessage(String lastCommitMessage) {
        this.lastCommitMessage = lastCommitMessage;
    }

    public String getLastCommitAuthor() {
        return lastCommitAuthor;
    }

    public void setLastCommitAuthor(String lastCommitAuthor) {
        this.lastCommitAuthor = lastCommitAuthor;
    }

    @JsonIgnore
    @Override
    public String getApi() {
        return super.getApi();
    }
}
