package it.pagopa.selfcare.product.entity;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.*;

public class Product {

  public static final String CONTRACT_TYPE_DEFAULT = "default";
  private String id;
  private String alias;
  private String logo;
  private String depictImageUrl;
  private String title;
  private String logoBgColor;
  private String description;
  private String urlPublic;
  private String urlBO;
  private Instant createdAt;
  private String createdBy;
  private Instant modifiedAt;
  private String modifiedBy;
  private Map<PartyRole, ProductRoleInfo> roleMappings;
  private Map<String, Map<PartyRole, ProductRoleInfo>> roleMappingsByInstitutionType;
  private String roleManagementURL;
  private boolean enabled = true;
  private boolean delegable;
  private boolean invoiceable;
  private ProductStatus status;
  private String parentId;
  private List<String> testEnvProductIds;
  private String identityTokenAudience;
  private Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations;
  private Product parent;
  private List<String> consumers;
  private Map<String, ContractTemplate> institutionContractMappings;
  private Map<String, ContractTemplate> userContractMappings;
  private Map<String, ContractTemplate> userAggregatorContractMappings;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLogo() {
    return logo;
  }

  public void setLogo(String logo) {
    this.logo = logo;
  }

  public String getDepictImageUrl() {
    return depictImageUrl;
  }

  public void setDepictImageUrl(String depictImageUrl) {
    this.depictImageUrl = depictImageUrl;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getLogoBgColor() {
    return logoBgColor;
  }

  public void setLogoBgColor(String logoBgColor) {
    this.logoBgColor = logoBgColor;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUrlPublic() {
    return urlPublic;
  }

  public void setUrlPublic(String urlPublic) {
    this.urlPublic = urlPublic;
  }

  public String getUrlBO() {
    return urlBO;
  }

  public void setUrlBO(String urlBO) {
    this.urlBO = urlBO;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Instant getModifiedAt() {
    return modifiedAt;
  }

  public void setModifiedAt(Instant modifiedAt) {
    this.modifiedAt = modifiedAt;
  }

  public String getModifiedBy() {
    return modifiedBy;
  }

  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  /**
   * This method returns roleMappings associate with a specific InstitutionType on
   * roleMappingsByInstitutionType map. In case none InstitutionType exists on
   * roleMappingsByInstitutionType, it returns roleMappings.
   *
   * @param institutionType InstitutionType
   * @return Map<PartyRole, ProductRoleInfo>
   */
  public Map<PartyRole, ProductRoleInfo> getRoleMappings(String institutionType) {
    if (Objects.nonNull(institutionType)
        && Objects.nonNull(roleMappingsByInstitutionType)
        && roleMappingsByInstitutionType.containsKey(institutionType)) {
      return roleMappingsByInstitutionType.get(institutionType);
    }
    return roleMappings;
  }

  public Map<PartyRole, List<ProductRoleInfo>> getAllRoleMappings() {
    Map<PartyRole, List<ProductRoleInfo>> roleInfoMap = new HashMap<>();
    Optional.ofNullable(roleMappings)
        .ifPresent(
            roleMappings ->
                roleMappings.forEach(
                    (key, value) -> {
                      List<ProductRoleInfo> productRoles = new ArrayList<>();
                      productRoles.add(value);
                      roleInfoMap.put(key, productRoles);
                    }));
    Optional.ofNullable(roleMappingsByInstitutionType)
        .map(Map::values)
        .ifPresent(
            items ->
                items.stream()
                    .map(Map::entrySet)
                    .forEach(
                        item ->
                            item.forEach(
                                entry -> {
                                  List<ProductRoleInfo> productRoles =
                                      roleInfoMap.getOrDefault(entry.getKey(), new ArrayList<>());
                                  productRoles.add(entry.getValue());
                                  roleInfoMap.put(entry.getKey(), productRoles);
                                })));
    return roleInfoMap;
  }

  public void setRoleMappings(Map<PartyRole, ProductRoleInfo> roleMappings) {
    this.roleMappings = roleMappings;
  }

  public void setRoleMappingsByInstitutionType(
      Map<String, Map<PartyRole, ProductRoleInfo>> roleMappingsByInstitutionType) {
    this.roleMappingsByInstitutionType = roleMappingsByInstitutionType;
  }

  public String getRoleManagementURL() {
    return roleManagementURL;
  }

  public void setRoleManagementURL(String roleManagementURL) {
    this.roleManagementURL = roleManagementURL;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isDelegable() {
    return delegable;
  }

  public void setDelegable(boolean delegable) {
    this.delegable = delegable;
  }

  public boolean isInvoiceable() {
    return invoiceable;
  }

  public void setInvoiceable(boolean invoiceable) {
    this.invoiceable = invoiceable;
  }

  public ProductStatus getStatus() {
    return status;
  }

  public void setStatus(ProductStatus status) {
    this.status = status;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public String getIdentityTokenAudience() {
    return identityTokenAudience;
  }

  public void setIdentityTokenAudience(String identityTokenAudience) {
    this.identityTokenAudience = identityTokenAudience;
  }

  public Map<String, BackOfficeConfigurations> getBackOfficeEnvironmentConfigurations() {
    return backOfficeEnvironmentConfigurations;
  }

  public void setBackOfficeEnvironmentConfigurations(
      Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations) {
    this.backOfficeEnvironmentConfigurations = backOfficeEnvironmentConfigurations;
  }

  public Product getParent() {
    return parent;
  }

  public void setParent(Product parent) {
    this.parent = parent;
  }

  public List<String> getTestEnvProductIds() {
    return testEnvProductIds;
  }

  public void setTestEnvProductIds(List<String> testEnvProductIds) {
    this.testEnvProductIds = testEnvProductIds;
  }

  public List<String> getConsumers() {
    return consumers;
  }

  public void setConsumers(List<String> consumers) {
    this.consumers = consumers;
  }

  public boolean canAddAdmin() {
    ContractTemplate userContractTemplate = getUserContractTemplate(CONTRACT_TYPE_DEFAULT);
    return StringUtils.isNotEmpty(userContractTemplate.getContractTemplatePath())
        && StringUtils.isNotEmpty(userContractTemplate.getContractTemplateVersion());
  }

  public Map<String, ContractTemplate> getInstitutionContractMappings() {
    return institutionContractMappings;
  }

  public void setInstitutionContractMappings(
      Map<String, ContractTemplate> institutionContractMappings) {
    this.institutionContractMappings = institutionContractMappings;
  }

  public Map<String, ContractTemplate> getUserContractMappings() {
    return userContractMappings;
  }

  public void setUserContractMappings(Map<String, ContractTemplate> userContractMappings) {
    this.userContractMappings = userContractMappings;
  }

  public Map<String, ContractTemplate> getUserAggregatorContractMappings() {
    return userAggregatorContractMappings;
  }

  public void setUserAggregatorContractMappings(Map<String, ContractTemplate> userAggregatorContractMappings) {
    this.userAggregatorContractMappings = userAggregatorContractMappings;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  /**
   * This method returns contractStorage associate with a specific InstitutionType. In case none
   * InstitutionType exists on contractMapping, it returns a valid ContractTemplate.
   *
   * @param institutionType InstitutionType
   * @return ContractTemplate
   */
  public ContractTemplate getUserContractTemplate(String institutionType) {
    ContractTemplate userContractTemplate = new ContractTemplate();
    if (Objects.nonNull(getUserContractMappings())) {
      if (Objects.nonNull(institutionType)
          && getUserContractMappings().containsKey(institutionType)) {
        userContractTemplate = getUserContractMappings().get(institutionType);
      } else if (getUserContractMappings().containsKey(CONTRACT_TYPE_DEFAULT)) {
        userContractTemplate = getUserContractMappings().get(CONTRACT_TYPE_DEFAULT);
      }
    }
    return userContractTemplate;
  }

  /**
   * This method returns contractStorage associate with a specific InstitutionType. In case none
   * InstitutionType exists on contractMapping, it returns a valid ContractTemplate.
   *
   * @param institutionType InstitutionType
   * @return ContractTemplate
   */
  public ContractTemplate getInstitutionContractTemplate(String institutionType) {
    ContractTemplate contractTemplate = new ContractTemplate();
    if (Objects.nonNull(getInstitutionContractMappings())) {
      if (Objects.nonNull(institutionType)
          && getInstitutionContractMappings().containsKey(institutionType)) {
        contractTemplate = getInstitutionContractMappings().get(institutionType);
      } else if (getInstitutionContractMappings().containsKey(CONTRACT_TYPE_DEFAULT)) {
        contractTemplate = getInstitutionContractMappings().get(CONTRACT_TYPE_DEFAULT);
      }
    }
    return contractTemplate;
  }

  /**
   * This method returns ContractTemplate associate with a specific InstitutionType for UserAggregatorContract.
   * In case none InstitutionType exists on contractMapping, it returns the default ContractTemplate.
   *
   * @param institutionType InstitutionType
   * @return ContractTemplate
   */
  public ContractTemplate getUserAggregatorContractTemplate(String institutionType) {
    ContractTemplate userAggregatorContractTemplate = new ContractTemplate();
    if (Objects.nonNull(getUserAggregatorContractMappings())) {
      if (Objects.nonNull(institutionType)
              && getUserAggregatorContractMappings().containsKey(institutionType)) {
        userAggregatorContractTemplate = getUserAggregatorContractMappings().get(institutionType);
      } else if (getUserAggregatorContractMappings().containsKey(CONTRACT_TYPE_DEFAULT)) {
        userAggregatorContractTemplate = getUserAggregatorContractMappings().get(CONTRACT_TYPE_DEFAULT);
      }
    }
    return userAggregatorContractTemplate;
  }
}
