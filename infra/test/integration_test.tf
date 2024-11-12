resource "azurerm_resource_group" "onboarding_fn_rg" {
  name     = "${local.app_name}-rg"
  location = var.location

  tags = var.tags
}

data "azurerm_key_vault" "key_vault" {
  resource_group_name = var.key_vault.resource_group_name
  name                = var.key_vault.name
}

data "azurerm_key_vault_secret" "apim_product_pn" {
  name         = "anac-ftp-known-host"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

/*
resource "azurerm_key_vault_access_policy" "keyvault_functions_access_policy" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = module.selc_onboarding_fn.system_identity_principal

  secret_permissions = [
    "Get",
  ]
}*/

data "azurerm_resource_group" "nat_rg" {
  name = "${local.base_domain_name}-nat-rg"
}

data "azurerm_resource_group" "vnet_rg" {
  name = "${local.base_domain_vnet_name}-vnet-rg"
}

/*resource "azurerm_key_vault_secret" "fn_primary_key" {
  name         = "fn-onboarding-primary-key"
  value        = module.selc_onboarding_fn.primary_key
  content_type = "text/plain"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}*/

data "github_repository" "repo" {
  full_name = "pagopa/selfcare-onboarding"
}

resource "github_repository_environment" "repo_environment" {
  repository  = data.github_repository.repo.name
  environment = "dev-ci"
}


resource "github_actions_environment_secret" "integration_environment" {
  repository  = data.github_repository.repo.name
  environment = github_repository_environment.repo_environment.environment
  secret_name = "integration_environment"
  encrypted_value = templatefile("Selfcare-Integration.postman_environment.json",
    {
      env       = "${local.env_url}"
      apimKeyPN = "ciao" //data.azurerm_key_vault_secret.apim_product_pn.value
    })
}
