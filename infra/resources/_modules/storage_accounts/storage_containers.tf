resource "azurerm_storage_container" "selc_documents_blob" {
  name                  = "${var.prefix}-${var.env_short}-${var.app_name}-blob"
  storage_account_id    = module.storage_account.id
  container_access_type = "private"
}