package it.pagopa.selfcare.entity;


public class Billing {
    private String vatNumber;
    private String recipientCode;
    private boolean publicServices;

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getRecipientCode() {
        return recipientCode;
    }

    public void setRecipientCode(String recipientCode) {
        this.recipientCode = recipientCode;
    }

    public boolean isPublicServices() {
        return publicServices;
    }

    public void setPublicServices(boolean publicServices) {
        this.publicServices = publicServices;
    }
}
