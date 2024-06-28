package it.pagopa.selfcare.onboarding.exception;

public class NotificationException extends RuntimeException {

    private final String code;

    public NotificationException(String message, String code) {
        super(message);
        this.code = code;
    }

    public NotificationException(String message) {
        super(message);
        this.code = "0000";
    }

    public String getCode() {
        return code;
    }
}
