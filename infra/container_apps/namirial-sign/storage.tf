resource "azapi_resource" "managed_environment_storages" {
  # count       = length(var.ca_volume_mounts) > 0 ? 1 : 0
  count       = 0
  type        = "Microsoft.App/managedEnvironments/storages@2022-03-01"
  name        = "swscustomazfile"   # must equals to ca_volume_mounts.volume_name without -
  parent_id   = data.azurerm_container_app_environment.container_app_environment.id

  body = jsonencode({
    properties = {
      azureFile = {
        accountName = data.azurerm_storage_account.selc_contracts_storage.name
        accountKey  = data.azurerm_storage_account.selc_contracts_storage.primary_access_key
        accessMode  = "ReadWrite"
        shareName   = "swsstorage"
      }
    }
  })
}

# Identity for Container App
#resource "azurerm_user_assigned_identity" "ca_identity" {
#  name                = "${local.project}-namirial-sign-identity"
#  resource_group_name = data.azurerm_resource_group.rg_container_app.name
#  location            = data.azurerm_resource_group.rg_container_app.location
#}

# Assign Contributor role to the Storage Account
#resource "azurerm_role_assignment" "role_contributor_ca" {
#  scope                = data.azurerm_storage_account.selc_contracts_storage.id
#  role_definition_name = "Contributor"
#  principal_id         = azurerm_user_assigned_identity.ca_identity.principal_id
#}