locals {
  project = "${var.prefix}-${var.env_short}"

  github = {
    org        = "pagopa"
    repository = "selfcare-onboarding"

    ci_branch_policy_enabled = var.github_repository_environment_ci.protected_branches == true || var.github_repository_environment_ci.custom_branch_policies == true
    cd_branch_policy_enabled = var.github_repository_environment_cd.protected_branches == true || var.github_repository_environment_cd.custom_branch_policies == true
  }

  repo_variables = {
    "AZURE_TENANT_ID" = data.azurerm_client_config.current.tenant_id,
  }

  repo_secrets = {
    "SONAR_TOKEN" = data.azurerm_key_vault_secret.key_vault_sonar.value,
  }

  env_ci_variables = {
    "AZURE_SUBSCRIPTION_ID" = data.azurerm_client_config.current.subscription_id
  }

  env_cd_variables = {
    "AZURE_SUBSCRIPTION_ID" = data.azurerm_client_config.current.subscription_id
  }

  env_ci_secrets = {
    "AZURE_CLIENT_ID" = module.identity_ci.identity_client_id
  }

  env_cd_secrets = {
    "AZURE_CLIENT_ID" = module.identity_cd.identity_client_id
  }
}
