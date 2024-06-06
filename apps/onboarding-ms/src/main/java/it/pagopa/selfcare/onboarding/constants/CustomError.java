package it.pagopa.selfcare.onboarding.constants;

public enum CustomError {

    DEFAULT_ERROR("0000", ""),

    ROLES_NOT_ADMITTED_ERROR("0034","Roles %s are not admitted for this operation"),
    AOO_NOT_FOUND("0000","AOO  %s not found"),
    UO_NOT_FOUND("0000","UO %s not found"),

    INSTITUTION_NOT_FOUND("0000","Institution  with taxCode %s origin %s originId % subunitCode %s not found"),
    USERS_UPDATE_NOT_ALLOWED("0025", "Invalid users information provided");

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
