package io.spaship.operator.gitapi;

public class GitlabApiException extends RuntimeException {
    public GitlabApiException() {
    }

    public GitlabApiException(String message) {
        super(message);
    }

    public GitlabApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitlabApiException(Throwable cause) {
        super(cause);
    }

    public GitlabApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
