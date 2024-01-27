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


cidr_subnet_selc_onboarding_fn = ["10.1.144.0/24"]

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
  "USER_REGISTRY_URL"                                  = "https://api.uat.pdv.pagopa.it/user-registry/v1",
  "MONGODB_CONNECTION_URI"                             = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/mongodb-connection-string/)",
  "USER_REGISTRY_API_KEY"                              = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/user-registry-api-key/)",
  "BLOB_STORAGE_CONN_STRING_PRODUCT"                   = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/blob-storage-product-connection-string/)",
  "STORAGE_CONTAINER_CONTRACT"                         = "selc-d-contracts-blob",
  "STORAGE_CONTAINER_PRODUCT"                          = "selc-d-product",
  "BLOB_STORAGE_CONN_STRING_CONTRACT"                  = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/contracts-storage-blob-connection-string/)",
  "MAIL_DESTINATION_TEST_ADDRESS"                      = "pectest@pec.pagopa.it",
  "MAIL_TEMPLATE_REGISTRATION_REQUEST_PT_PATH"         = "contracts/template/mail/registration-request-pt/1.0.0.json",
  "MAIL_SERVER_USERNAME"                               = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/smtp-not-pec-usr/)",
  "MAIL_SERVER_PASSWORD"                               = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/smtp-not-pec-psw/)",
  "MAIL_TEMPLATE_REGISTRATION_NOTIFICATION_ADMIN_PATH" = "contracts/template/mail/registration-notification-admin/1.0.0.json",
  "MAIL_TEMPLATE_NOTIFICATION_PATH"                    = "contracts/template/mail/onboarding-notification/1.0.0.json",
  "ADDRESS_EMAIL_NOTIFICATION_ADMIN"                   = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/portal-admin-operator-email/)",
  "MAIL_TEMPLATE_COMPLETE_PATH"                        = "contracts/template/mail/onboarding-complete/1.0.0.json",
  "MAIL_TEMPLATE_FD_COMPLETE_NOTIFICATION_PATH"        = "contracts/template/mail/onboarding-complete-fd/1.0.0.json",
  "MAIL_TEMPLATE_AUTOCOMPLETE_PATH"                    = "contracts/template/mail/import-massivo-io/1.0.0.json",
  "MAIL_TEMPLATE_DELEGATION_NOTIFICATION_PATH"         = "contracts/template/mail/delegation-notification/1.0.0.json",
  "MAIL_TEMPLATE_REGISTRATION_PATH"                    = "contracts/template/mail/1.0.0.json",
  "MAIL_TEMPLATE_REJECT_PATH"                          = "contracts/template/mail/onboarding-refused/1.0.0.json",
  "MAIL_TEMPLATE_PT_COMPLETE_PATH"                     = "contracts/template/mail/registration-complete-pt/1.0.0.json",
  "SELFCARE_ADMIN_NOTIFICATION_URL" : "https://dev.selfcare.pagopa.it/dashboard/admin/onboarding/",
  "SELFCARE_URL"                      = "https://selfcare.pagopa.it",
  "MAIL_ONBOARDING_CONFIRMATION_LINK" = "https://dev.selfcare.pagopa.it/onboarding/confirm?jwt=",
  "MAIL_ONBOARDING_REJECTION_LINK"    = "https://dev.selfcare.pagopa.it/onboarding/cancel?jwt=",
  "MAIL_ONBOARDING_URL" : "https://dev.selfcare.pagopa.it/onboarding/",
  "MS_CORE_URL"      = "https://selc.internal.dev.selfcare.pagopa.it/ms-core/v1",
  "JWT_BEARER_TOKEN" = "@Microsoft.KeyVault(SecretUri=https://selc-d-kv.vault.azure.net/secrets/jwt-bearer-token-functions/)"
}