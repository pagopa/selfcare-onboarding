data "azurerm_subnet" "documents_sa_pep" {
  name = provider::dx::resource_name(merge(local.naming_config, { domain = null, name = "pep", resource_type = "subnet" }))

  virtual_network_name = data.azurerm_virtual_network.vnet_selc.name
  resource_group_name  = data.azurerm_virtual_network.vnet_selc.resource_group_name
}

resource "azurerm_resource_group" "documents_sa_rg" {
  name = "${local.project}-${local.naming_config}-storage-rg"
  //provider::dx::resource_name(merge(local.naming_config, { resource_type = "resource_group" }))
  location = var.location
}

resource "azurerm_subnet" "documents_snet" {
  name                 = "${local.naming_config}-subnet"
  virtual_network_name = data.azurerm_virtual_network.vnet_selc.name
  resource_group_name  = data.azurerm_virtual_network.vnet_selc.resource_group_name
  address_prefixes     = ["10.50.246.0/24"]
}

resource "azurerm_user_assigned_identity" "documents_identity" {
  name                = "${local.naming_config}-identity"
  resource_group_name = azurerm_resource_group.documents_sa_rg.name
  location            = var.location
}

module "storage_documents" {
  source = "../_modules/storage_accounts"

  prefix          = local.prefix
  env_short       = local.env_short
  location        = local.location
  domain          = ""
  app_name        = local.prefix
  instance_number = "01"

  virtual_network_name           = data.azurerm_virtual_network.vnet_selc.resource_group_name
  virtual_network_resource_group = data.azurerm_virtual_network.vnet_selc.name

  resource_group_name = azurerm_resource_group.documents_sa_rg

  subnet_pep_id = azurerm_subnet.documents_snet.id

  tags = local.tags

  cidr_subnet_contract_storage = []
  key_vault_id = data.azurerm_key_vault.key_vault.id

  project = local.prefix
  storageName = "${local.prefix}${local.env_short}${local.naming_config}sa"
  subscription = data.azurerm_subscription.current.id
}

/*module "storage_documents" {
  source  = "pagopa-dx/azure-storage-account/azurerm"
  version = "~>1.0"

  environment = {
    prefix    = var.prefix
    env_short = var.env_short
    location = var.location
    //    domain          = var.domain
    app_name  = local.naming_config
    //    instance_number = "01"
  }

  resource_group_name                  = azurerm_resource_group.documents_sa_rg
  subnet_pep_id                        = data.azurerm_subnet.documents_sa_pep.id
  private_dns_zone_resource_group_name = data.azurerm_virtual_network.vnet_selc.resource_group_name
  force_public_network_access_enabled  = false

  blob_features = {
    immutability_policy = {
      enabled                       = true
      allow_protected_append_writes = true
      period_since_creation_in_days = 1
    }
    # restore_policy_days   = 30 # Cannot enable both immutability_policy and restore_policy
    delete_retention_days = 1
    versioning            = true
    last_access_time      = true
    change_feed = {
      enabled           = true
      retention_in_days = 30
    }
  }

  static_website = {
    enabled = false
  }

  tier = "l"

  tags = var.tags
}
*/
module "storage_documents_role_assignments_ms" {
  source  = "pagopa-dx/azure-role-assignments/azurerm"
  version = "~>1.0"

  principal_id = data.azurerm_container_app.container_app_onboarding_ms

  storage_blob = [
    {
      storage_account_name = module.storage_documents.name
      resource_group_name  = azurerm_resource_group.documents_sa_rg
      role                 = "writer"
    }
  ]
}