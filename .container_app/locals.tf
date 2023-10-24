locals {
  project  = "${var.prefix}-${var.env_short}"
  app_name = "onboarding"

  secrets = [for secret in var.key_vault.secrets_names :
    {
      identity    = "system"
      name        = "${secret}"
      keyVaultUrl = data.azurerm_key_vault_secret.keyvault_secret["${secret}"].id
  }]

  # secrets_env = [for secret in var.key_vault.secrets_names :
  #   {
  #     name      = upper(secret)
  #     secretRef = secret
  # }]
  secrets_env = [
    {
      name      = "JWT_TOKEN_PUBLIC_KEY"
      secretRef = "jwt-public-key"
    },
    {
      name      = "MONGODB_CONNECTION_URI"
      secretRef = "mongodb-connection-string"
    },
    {
      name      = "USER_REGISTRY_API_KEY"
      secretRef = "user-registry-api-key"
    },
    {
      name      = "ONBOARDING_FUNCTIONS_API_KEY"
      secretRef = "onboarding-functions-api-key"
    },
  ]
}