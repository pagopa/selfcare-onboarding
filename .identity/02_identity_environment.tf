
resource "azurerm_resource_group" "identity" {
  name     = "${local.product}-identity-rg"
  location = local.location
}

resource "azurerm_user_assigned_identity" "environment" {
  location            = azurerm_resource_group.identity.location
  name                = "${local.app_name}"
  resource_group_name = azurerm_resource_group.identity.name
}

resource "azurerm_federated_identity_credential" "environment" {
  name                = "github-federated"
  resource_group_name = azurerm_resource_group.identity.name
  audience            = ["api://AzureADTokenExchange"]
  issuer              = "https://token.actions.githubusercontent.com"
  parent_id           = azurerm_user_assigned_identity.environment.id
  subject             = "repo:${local.github.org}/${local.github.repository}:environment:${var.env}"
}

output "azure_environment" {
  value = {
    app_name       = "${local.app_name}"
    client_id      = azurerm_user_assigned_identity.environment.client_id
    application_id = azurerm_user_assigned_identity.environment.client_id
    object_id      = azurerm_user_assigned_identity.environment.principal_id
  }
}