locals {
  project = "${var.prefix}-${var.env_short}"

  github = {
    org        = "pagopa"
    repository = "selfcare-onboarding"
  }

  repo_secrets = {
    "AZURE_SUBSCRIPTION_ID" = data.azurerm_client_config.current.subscription_id
    "AZURE_TENANT_ID"       = data.azurerm_client_config.current.tenant_id,
    "SONAR_TOKEN"           = data.azurerm_key_vault_secret.key_vault_sonar.value,
  }

  env_cd_variables = {
    "AZURE_ONBOARDING_FN_APP_NAME"       = "${local.project}-onboarding-fn",
    "AZURE_ONBOARDING_FN_RESOURCE_GROUP" = "${local.project}-onboarding-fn-rg",
    "AZURE_ONBOARDING_FN_SERVICE_PLAN"   = "${local.project}-onboarding-fn-plan"
  }

  env_ci_secrets = {
    "AZURE_CLIENT_ID_CI" = module.identity_ci.identity_client_id
  }

  env_cd_secrets = {
    "AZURE_CLIENT_ID_CD" = module.identity_cd.identity_client_id
  }
}
