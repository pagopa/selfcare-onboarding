module "storage_account" {
  source  = "pagopa-dx/azure-storage-account/azurerm"
  version = "~>1.0"

  subnet_pep_id       = var.subnet_pep_id
  tags                = var.tags
  tier                = "l"
  environment         = local.environment
  resource_group_name = var.resource_group_name

  subservices_enabled = {
    blob  = true
    file  = false
    queue = true
    table = true
  }
}

resource "azurerm_key_vault_secret" "st_connection_string" {
  name         = "${var.storageName}StorageConnectionString"
  value        = module.storage_account.primary_connection_string
  key_vault_id = var.key_vault_id
  content_type = "connection string"
}