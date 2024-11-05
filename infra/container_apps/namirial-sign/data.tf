data "azurerm_resource_group" "rg_contracts_storage" {
  name = "${local.project}-contracts-storage-rg"
}

data "azurerm_key_vault" "key_vault" {
  name                = "${local.project}-kv"
  resource_group_name = "${local.project}-sec-rg"
}

data "azurerm_key_vault_secret" "hub_docker_user" {
  name         = "hub-docker-user"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "hub_docker_pwd" {
  name         = "hub-docker-pwd"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_log_analytics_workspace" "log_analytics" {
  name                = "${local.project}-law"
  resource_group_name = "${local.project}-monitor-rg"
}