data "azurerm_resource_group" "rg_contracts_storage" {
  name     = "${local.project}-contracts-storage-rg"
}

data "azurerm_resource_group" "rg_container_app" {
  name     = "${local.project}-container-app${var.suffix_increment}-rg"
}

data "azurerm_storage_account" "selc_contracts_storage" {
  name                = replace(format("%s-contracts-storage", local.project), "-", "")
  resource_group_name = "${local.project}-contracts-storage-rg"
}

data "azurerm_container_app_environment" "container_app_environment" {
  resource_group_name = local.ca_resource_group_name
  name                = local.container_app_environment_name
}