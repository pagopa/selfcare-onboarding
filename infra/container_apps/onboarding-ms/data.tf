data "azurerm_resource_group" "resource_group_app" {
  name = "${local.project}-container-app-rg"
}

data "azurerm_key_vault" "key_vault" {
  resource_group_name = var.key_vault.resource_group_name
  name                = var.key_vault.name
}

data "azurerm_key_vault_secrets" "key_vault_secrets" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "keyvault_secret" {
  for_each     = toset(data.azurerm_key_vault_secrets.key_vault_secrets.names)
  name         = each.key
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_container_app_environment" "container_app_environment" {
  resource_group_name = data.azurerm_resource_group.resource_group_app.name
  name                = "${local.project}-cae"
}

data "azurerm_resource_group" "rg_vnet" {
  name = format("%s-vnet-rg", local.project)
}

data "azurerm_private_dns_zone" "private_azurecontainerapps_io" {
  name                = local.container_app_environment_dns_zone_name
  resource_group_name = data.azurerm_resource_group.rg_vnet.name
}
