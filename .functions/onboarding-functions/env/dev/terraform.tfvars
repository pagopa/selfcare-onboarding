prefix    = "selc"
env_short = "d"
location  = "westeurope"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

key_vault = {
  resource_group_name = "selc-d-sec-rg"
  name                = "selc-d-kv"
}


cidr_subnet_selc_onboarding_fn        = ["10.1.144.0/24"]

function_always_on = false

app_service_plan_info = {
  kind                         = "Linux"
  sku_size                     = "P1v3"
  sku_tier                     = "PremiumV3"
  maximum_elastic_worker_count = 1
  worker_count                 = 1
  zone_balancing_enabled       = false
}

storage_account_info = {
  account_kind                      = "StorageV2"
  account_tier                      = "Standard"
  account_replication_type          = "LRS"
  access_tier                       = "Hot"
  advanced_threat_protection_enable = false
}

app_settings = {
  "USER_REGISTRY_URL" = "https://api.uat.pdv.pagopa.it/user-registry/v1",
  "MONGODB_CONNECTION_URI" = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/mongodb-connection-string/)",
  "USER_REGISTRY_API_KEY" = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/user-registry-api-key/)",
  "BLOB_STORAGE_CONN_STRING_PRODUCT" = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/blob-storage-product-connection-string/)",
  "STORAGE_CONTAINER_CONTRACT" = "selc-d-contracts-blob",
  "STORAGE_CONTAINER_PRODUCT" = "selc-d-product",
  "BLOB_STORAGE_CONN_STRING_CONTRACT" = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/contracts-storage-blob-connection-string/)",
  "MAIL_DESTINATION_TEST_ADDRESS" = "pectest@pec.pagopa.it",
  "MAIL_TEMPLATE_REGISTRATION_REQUEST_PT_PATH" = "contracts/template/mail/registration-request-pt/1.0.0.json",
  "MAIL_SERVER_USERNAME" = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/smtp-usr/)",
  "MAIL_SERVER_PASSWORD" = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/smtp-psw/)",
  "MAIL_TEMPLATE_REGISTRATION_NOTIFICATION_ADMIN_PATH" = "contracts/template/mail/registration-notification-admin/1.0.0.json",
  "MAIL_TEMPLATE_NOTIFICATION_PATH" = "contracts/template/mail/onboarding-notification/1.0.0.json"
}
