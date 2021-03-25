package io.spaship.operator.openshift;

public class WebsiteConfigEnvException extends Exception {
    public WebsiteConfigEnvException() {
        super();
    }

    public WebsiteConfigEnvException(String message) {
        super(message);
    }

    public WebsiteConfigEnvException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebsiteConfigEnvException(Throwable cause) {
        super(cause);
    }

    protected WebsiteConfigEnvException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
