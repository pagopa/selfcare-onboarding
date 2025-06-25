resource "azurerm_storage_container" "example" {
  name                  = "${var.prefix}-${var.env_short}-${var.app_name}-blob"
  storage_account_id    = module.storage_account.id
  container_access_type = "private"
}