package it.pagopa.selfcare.product.entity;

import java.util.List;

public class ContractTemplate {

  private String contractTemplatePath;
  private String contractTemplateVersion;
  private List<AttachmentTemplate> attachmentMappings;

  public List<AttachmentTemplate> getAttachmentMappings() {
    return attachmentMappings;
  }

  public void setAttachmentMappings(List<AttachmentTemplate> attachmentMappings) {
    this.attachmentMappings = attachmentMappings;
  }

  public String getContractTemplatePath() {
    return contractTemplatePath;
  }

  public void setContractTemplatePath(String contractTemplatePath) {
    this.contractTemplatePath = contractTemplatePath;
  }

  public String getContractTemplateVersion() {
    return contractTemplateVersion;
  }

  public void setContractTemplateVersion(String contractTemplateVersion) {
    this.contractTemplateVersion = contractTemplateVersion;
  }
}
