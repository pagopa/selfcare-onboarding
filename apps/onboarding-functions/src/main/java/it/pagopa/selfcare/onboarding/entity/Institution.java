package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class Institution {

    @BsonProperty("id")
    private String id;
    private InstitutionType institutionType;
    private String taxCode;
    private String subunitCode;
    private InstitutionPaSubunitType subunitType;

    private Origin origin;
    private String originId;
    private String city;
    private String country;
    private String county;
    private String description;
    private String digitalAddress;
    private String address;
    private String zipCode;

    private List<GeographicTaxonomy> geographicTaxonomies;

    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;

    private String supportEmail;
    private String supportPhone;

    private boolean imported;

    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private GPUData gpuData;
    private String parentDescription;

    public InstitutionType getInstitutionType() {
        return institutionType;
    }

    public void setInstitutionType(InstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getSubunitCode() {
        return subunitCode;
    }

    public void setSubunitCode(String subunitCode) {
        this.subunitCode = subunitCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDigitalAddress() {
        return digitalAddress;
    }

    public void setDigitalAddress(String digitalAddress) {
        this.digitalAddress = digitalAddress;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public List<GeographicTaxonomy> getGeographicTaxonomies() {
        return geographicTaxonomies;
    }

    public void setGeographicTaxonomies(List<GeographicTaxonomy> geographicTaxonomies) {
        this.geographicTaxonomies = geographicTaxonomies;
    }

    public String getRea() {
        return rea;
    }

    public void setRea(String rea) {
        this.rea = rea;
    }

    public String getShareCapital() {
        return shareCapital;
    }

    public void setShareCapital(String shareCapital) {
        this.shareCapital = shareCapital;
    }

    public String getBusinessRegisterPlace() {
        return businessRegisterPlace;
    }

    public void setBusinessRegisterPlace(String businessRegisterPlace) {
        this.businessRegisterPlace = businessRegisterPlace;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getSupportPhone() {
        return supportPhone;
    }

    public void setSupportPhone(String supportPhone) {
        this.supportPhone = supportPhone;
    }

    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }

    public PaymentServiceProvider getPaymentServiceProvider() {
        return paymentServiceProvider;
    }

    public void setPaymentServiceProvider(PaymentServiceProvider paymentServiceProvider) {
        this.paymentServiceProvider = paymentServiceProvider;
    }

    public DataProtectionOfficer getDataProtectionOfficer() {
        return dataProtectionOfficer;
    }

    public void setDataProtectionOfficer(DataProtectionOfficer dataProtectionOfficer) {
        this.dataProtectionOfficer = dataProtectionOfficer;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public String getOriginId() {
        return originId;
    }

    public void setOriginId(String originId) {
        this.originId = originId;
    }

    public InstitutionPaSubunitType getSubunitType() {
        return subunitType;
    }

    public void setSubunitType(InstitutionPaSubunitType subunitType) {
        this.subunitType = subunitType;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentDescription() {
        return parentDescription;
    }

    public void setParentDescription(String parentDescription) {
        this.parentDescription = parentDescription;
    }

    public GPUData getGpuData() {
        return gpuData;
    }

    public void setGpuData(GPUData gpuData) {
        this.gpuData = gpuData;
    }

    @Override
    public String toString() {
        return "Institution{" +
                "id='" + id + '\'' +
                ", taxCode='" + taxCode + '\'' +
                ", description=" + description +
                ", digitalAddress=" + digitalAddress +
                ", origin=" + origin +
                ", originId=" + originId +
                '}';
    }
}
