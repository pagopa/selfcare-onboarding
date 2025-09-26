package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import java.util.List;
import java.util.Optional;

import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import lombok.Data;
import org.bson.codecs.pojo.annotations.BsonProperty;

@Data
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
    private String istatCode;
    private String description;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String legalForm;

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
    private List<String> atecoCodes;

    public void encryptTaxCode(String taxCode) {
        this.taxCode = Optional.ofNullable(taxCode)
                .map(DataEncryptionUtils::encrypt)
                .orElse("");
    }

    public String decryptTaxCode() {
        return Optional.ofNullable(taxCode)
                .map(DataEncryptionUtils::decrypt)
                .orElse("");
    }

    public void encryptOriginId(String originId) {
        this.originId = Optional.ofNullable(originId)
                .map(DataEncryptionUtils::encrypt)
                .orElse("");
    }

    public String decryptOriginId() {
        return Optional.ofNullable(originId)
                .map(DataEncryptionUtils::decrypt)
                .orElse("");
    }

}
