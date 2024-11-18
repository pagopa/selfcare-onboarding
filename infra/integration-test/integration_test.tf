data "azurerm_key_vault" "key_vault" {
  resource_group_name = var.key_vault.resource_group_name
  name                = var.key_vault.name
}

data "azurerm_key_vault_secret" "apim_product_pn_sk" {
  name         = "apim-product-pn-sk"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_resource_group" "nat_rg" {
  name = "${local.base_domain_name}-nat-rg"
}

data "azurerm_resource_group" "vnet_rg" {
  name = "${local.base_domain_vnet_name}-vnet-rg"
}

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
  plaintext_value = base64encode(templatefile("Selfcare-Integration.postman_environment.json",
    {
      env       = "${local.env_url}"
      apimKeyPN = data.azurerm_key_vault_secret.apim_product_pn_sk.value
    }))
}
