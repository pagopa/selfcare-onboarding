locals {
  project  = "${var.prefix}-${var.env_short}"
  app_name = "onboarding-ms"

  secrets = [for secret in var.key_vault.secrets_names :
    {
      identity    = "system"
      name        = "${secret}"
      keyVaultUrl = data.azurerm_key_vault_secret.keyvault_secret["${secret}"].id
  }]

  secrets_env = [for secret in var.key_vault.secrets_names :
    {
      name      = upper(secret)
      secretRef = secret
  }]
}