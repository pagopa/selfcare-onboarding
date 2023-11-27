
data "azurerm_resource_group" "rg_vnet" {
  name = format("%s-vnet-rg", local.project)
}

data "azurerm_resource_group" "rg_monitor" {
  name = local.monitor_rg_name
}

data "azurerm_application_insights" "application_insights" {
  name                = local.monitor_appinsights_name
  resource_group_name = data.azurerm_resource_group.rg_monitor.name
}

data "azurerm_virtual_network" "vnet" {
  name                 = format("%s-vnet", local.project)
  resource_group_name  = data.azurerm_resource_group.rg_vnet.name
}

resource "azurerm_resource_group" "onboarding_fn_rg" {
  name     = "${local.project}-onboarding-fn-rg"
  location = var.location

  tags = var.tags
}


# subnet
module "onboarding_fn_snet" {
  count                = var.cidr_subnet_selc_onboarding_fn != null ? 1 : 0
  source               = "git::https://github.com/pagopa/terraform-azurerm-v3.git//subnet?ref=v7.8.0"
  name                 = format("%s-onboarding-fn-snet", local.project)
  resource_group_name  = data.azurerm_resource_group.rg_vnet.name
  virtual_network_name = data.azurerm_virtual_network.vnet.name
  address_prefixes     = var.cidr_subnet_selc_onboarding_fn

  delegation = {
    name = "default"
    service_delegation = {
      name    = "Microsoft.Web/serverFarms"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}

module "selc_onboarding_fn" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//function_app?ref=v7.8.0"

  name                = format("%s-onboarding-fn", local.project)
  location            = azurerm_resource_group.onboarding_fn_rg.location
  resource_group_name = azurerm_resource_group.onboarding_fn_rg.name

  health_check_path                        = "/api/v1/info"
  always_on                                = var.function_always_on
  subnet_id                                = module.onboarding_fn_snet[0].id
  application_insights_instrumentation_key = data.azurerm_application_insights.application_insights.instrumentation_key
  java_version                             = "17"
  runtime_version                          = "~4"

  system_identity_enabled = true

  storage_account_name = replace(format("%s-onboarding-fn-storage", local.project), "-", "")

  app_service_plan_info = var.app_service_plan_info
  storage_account_info = var.storage_account_info

  app_settings = var.app_settings

  tags = var.tags
}

data "azurerm_key_vault" "key_vault" {
  resource_group_name = var.key_vault.resource_group_name
  name                = var.key_vault.name
}

resource "azurerm_key_vault_access_policy" "keyvault_functions_access_policy" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = module.selc_onboarding_fn.system_identity_principal

  secret_permissions = [
    "Get",
  ]
}
