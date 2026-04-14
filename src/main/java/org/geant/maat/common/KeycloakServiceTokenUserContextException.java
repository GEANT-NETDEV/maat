package org.geant.maat.common;

public class KeycloakServiceTokenUserContextException extends RuntimeException {
    private final int httpStatus;

    public KeycloakServiceTokenUserContextException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
