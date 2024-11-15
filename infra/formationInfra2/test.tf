data "azurerm_key_vault" "first_try" {
  name                = var.key_vault.name
  resource_group_name = var.key_vault.resource_group_name
}

data "azurerm_key_vault_secret" "formation_reader" {
  name         = "formation"
  key_vault_id = data.azurerm_key_vault.first_try.id
}

resource "azurerm_key_vault_secret" "formation_writer" {
  name         = "formation2"
  value        = data.azurerm_key_vault_secret.formation_reader.value
  key_vault_id = data.azurerm_key_vault.first_try.id
}