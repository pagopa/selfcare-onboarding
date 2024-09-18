package it.pagopa.selfcare.onboarding.entity;


import it.pagopa.selfcare.onboarding.common.Origin;


import java.util.List;


public class AggregateInstitution {

    private String taxCode;
    private String description;
    private String subunitCode;
    private String subunitType;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private String address;
    private String zipCode;
    private String originId;
    private Origin origin;
    private List<User> users;

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public List<User> getUsers() {return users;}

    public void setUsers(List<User> users) {this.users = users;}

    public String getSubunitCode() {
        return subunitCode;
    }

    public void setSubunitCode(String subunitCode) {
        this.subunitCode = subunitCode;
    }

    public String getSubunitType() {
        return subunitType;
    }

    public void setSubunitType(String subunitType) {
        this.subunitType = subunitType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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


}
