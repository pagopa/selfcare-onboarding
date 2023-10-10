data "github_organization_teams" "all" {
  root_teams_only = true
  summary_only    = true
}

data "azurerm_key_vault" "key_vault" {
  name                = "${local.prefix}-${var.env_short}-kv"
  resource_group_name = "${local.prefix}-${var.env_short}-sec-rg"
}

data "azurerm_key_vault_secret" "jwt_public_key" {
  name         = "jwt-public-key"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "mongodb_connection_string" {
  name         = "mongodb-connection-string"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "user_registry_api_key" {
  name         = "user-registry-api-key"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}
