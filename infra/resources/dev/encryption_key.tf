resource "random_password" "encryption_key" {
  length           = 32
  special          = false
  override_characters = ""
}

resource "azurerm_key_vault_secret" "encryption_key_secret" {
  name         = "backend-encryption-key"
  value        = random_password.encryption_key.result
  content_type = "text/plain"

  key_vault_id = data.azurerm_key_vault.key_vault.id
}