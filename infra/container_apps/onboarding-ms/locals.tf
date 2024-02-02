locals {
  pnpg_suffix = var.is_pnpg == true ? "-pnpg" : ""
  project     = "${var.prefix}-${var.env_short}"

  resource_group_name                     = "${local.project}-container-app-rg"
  vnet_name                               = "${local.project}-vnet-rg"
  key_vault_resource_group_name           = "${local.project}${local.pnpg_suffix}-sec-rg"
  key_vault_name                          = "${local.project}${local.pnpg_suffix}-kv"
  container_app_environment_name          = "${local.project}${local.pnpg_suffix}-cae"
  app_name                                = "${local.project}${local.pnpg_suffix}-onboarding-ms-ca"
  container_app_environment_dns_zone_name = "azurecontainerapps.io"

  secrets = [for secret in var.secrets_names :
    {
      identity    = "system"
      name        = "${secret}"
      keyVaultUrl = data.azurerm_key_vault_secret.keyvault_secret["${secret}"].id
  }]

  secrets_env = [for secret in var.secrets_names :
    {
      name      = upper(secret)
      secretRef = secret
  }]
}