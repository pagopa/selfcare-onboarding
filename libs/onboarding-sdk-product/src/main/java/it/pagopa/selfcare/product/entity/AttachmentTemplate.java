package it.pagopa.selfcare.product.entity;

public class AttachmentTemplate {

  private String templatePath;
  private String templateVersion;
  private int order;

  public String getTemplatePath() {
    return templatePath;
  }

  public void setTemplatePath(String templatePath) {
    this.templatePath = templatePath;
  }

  public String getTemplateVersion() {
    return templateVersion;
  }

  public void setTemplateVersion(String templateVersion) {
    this.templateVersion = templateVersion;
  }
}
