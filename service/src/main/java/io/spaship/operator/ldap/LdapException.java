package io.spaship.operator.ldap;

public class LdapException extends RuntimeException {
    public LdapException() {
    }

    public LdapException(String message) {
        super(message);
    }

    public LdapException(String message, Throwable cause) {
        super(message, cause);
    }

    public LdapException(Throwable cause) {
        super(cause);
    }

    public LdapException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
