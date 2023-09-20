package it.pagopa.selfcare.constants;

public enum CustomError {

    ROLES_NOT_ADMITTED_ERROR("0034","Roles %s are not admitted for this operation");

    private final String code;
    private final String detail;

    CustomError(String code, String detail) {
        this.code = code;
        this.detail = detail;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return detail;
    }
}
