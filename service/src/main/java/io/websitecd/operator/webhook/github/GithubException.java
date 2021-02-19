package io.websitecd.operator.webhook.github;

public class GithubException extends RuntimeException {
    public GithubException() {
    }

    public GithubException(String message) {
        super(message);
    }

    public GithubException(String message, Throwable cause) {
        super(message, cause);
    }

    public GithubException(Throwable cause) {
        super(cause);
    }

    public GithubException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
