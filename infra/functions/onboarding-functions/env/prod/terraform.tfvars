prefix    = "selc"
env_short = "p"
location  = "westeurope"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

key_vault = {
  resource_group_name = "selc-p-sec-rg"
  name                = "selc-p-kv"
}


cidr_subnet_selc_onboarding_fn = ["10.1.144.0/24"]

function_always_on = true

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
  advanced_threat_protection_enable = true
}

app_settings = {
  "USER_REGISTRY_URL"                                  = "https://api.pdv.pagopa.it/user-registry/v1",
  "MONGODB_CONNECTION_URI"                             = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/mongodb-connection-string/)",
  "USER_REGISTRY_API_KEY"                              = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/user-registry-api-key/)",
  "BLOB_STORAGE_CONN_STRING_PRODUCT"                   = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/web-storage-blob-connection-string/)",
  "STORAGE_CONTAINER_CONTRACT"                         = "selc-p-contracts-blob",
  "STORAGE_CONTAINER_PRODUCT"                          = "selc-p-product",
  "BLOB_STORAGE_CONN_STRING_CONTRACT"                  = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/contracts-storage-blob-connection-string/)",
  "MAIL_DESTINATION_TEST"                              = "false",
  "MAIL_DESTINATION_TEST_ADDRESS"                      = "pectest@pec.pagopa.it",
  "MAIL_TEMPLATE_REGISTRATION_REQUEST_PT_PATH"         = "contracts/template/mail/registration-request-pt/1.0.0.json",
  "MAIL_SENDER_ADDRESS"                                = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/smtp-usr/)",
  "MAIL_SERVER_USERNAME"                               = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/smtp-usr/)",
  "MAIL_SERVER_PASSWORD"                               = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/smtp-psw/)",
  "MAIL_SERVER_HOST"                                   = "smtps.pec.aruba.it",
  "MAIL_SERVER_PORT"                                   = "465",
  "MAIL_TEMPLATE_REGISTRATION_NOTIFICATION_ADMIN_PATH" = "contracts/template/mail/registration-notification-admin/1.0.0.json",
  "MAIL_TEMPLATE_NOTIFICATION_PATH"                    = "contracts/template/mail/onboarding-notification/1.0.0.json",
  "ADDRESS_EMAIL_NOTIFICATION_ADMIN"                   = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/portal-admin-operator-email/)",
  "MAIL_TEMPLATE_COMPLETE_PATH"                        = "contracts/template/mail/onboarding-complete/1.0.0.json",
  "MAIL_TEMPLATE_FD_COMPLETE_NOTIFICATION_PATH"        = "contracts/template/mail/onboarding-complete-fd/1.0.0.json",
  "MAIL_TEMPLATE_PT_COMPLETE_PATH"                     = "contracts/template/mail/registration-complete-pt/1.0.0.json",
  "MAIL_TEMPLATE_AUTOCOMPLETE_PATH"                    = "contracts/template/mail/import-massivo-io/1.0.0.json",
  "MAIL_TEMPLATE_DELEGATION_NOTIFICATION_PATH"         = "contracts/template/mail/delegation-notification/1.0.0.json",
  "MAIL_TEMPLATE_REGISTRATION_PATH"                    = "contracts/template/mail/onboarding-request/1.0.1.json",
  "MAIL_TEMPLATE_REJECT_PATH"                          = "contracts/template/mail/onboarding-refused/1.0.1.json",
  "SELFCARE_ADMIN_NOTIFICATION_URL"                    = "https://selfcare.pagopa.it/dashboard/admin/onboarding/",
  "SELFCARE_URL"                                       = "https://selfcare.pagopa.it",
  "MAIL_ONBOARDING_CONFIRMATION_LINK"                  = "https://selfcare.pagopa.it/onboarding/confirm?jwt=",
  "MAIL_ONBOARDING_REJECTION_LINK"                     = "https://selfcare.pagopa.it/onboarding/cancel?jwt=",
  "MAIL_ONBOARDING_URL"                                = "https://selfcare.pagopa.it/onboarding/",
  "MS_CORE_URL"                                        = "https://selc-p-ms-core-ca.greensand-62fc96da.westeurope.azurecontainerapps.io",
  "JWT_BEARER_TOKEN"                                   = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/jwt-bearer-token-functions/)",
  "MS_USER_URL"                                        = "https://selc-p-user-ms-ca.greensand-62fc96da.westeurope.azurecontainerapps.io",
  "MS_PARTY_REGISTRY_URL"                              = "https://selc-p-party-reg-proxy-ca.greensand-62fc96da.westeurope.azurecontainerapps.io",
  "USER_MS_ACTIVE"                                     = "false"

  ##ARUBA SIGNATURE
  "PAGOPA_SIGNATURE_SOURCE"                        = "aruba",
  "ARUBA_SIGN_SERVICE_IDENTITY_TYPE_OTP_AUTH"      = "faPagoPa",
  "ARUBA_SIGN_SERVICE_IDENTITY_OTP_PWD"            = "dsign",
  "ARUBA_SIGN_SERVICE_IDENTITY_USER"               = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/aruba-sign-service-user/)",
  "ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_USER"     = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/aruba-sign-service-delegated-user/)",
  "ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_PASSWORD" = "@Microsoft.KeyVault(SecretUri=https://selc-p-kv.vault.azure.net/secrets/aruba-sign-service-delegated-psw/)",
  "ARUBA_SIGN_SERVICE_IDENTITY_DELEGATED_DOMAIN"   = "faPagoPa",
  "ARUBA_SIGN_SERVICE_BASE_URL"                    = "https://asbr-pagopa.arubapec.it/ArubaSignService/ArubaSignService"
  "ARUBA_SIGN_SERVICE_REQUEST_TIMEOUT_MS"          = "60000"
  "ARUBA_SIGN_SERVICE_CONNECT_TIMEOUT_MS"          = "60000"
}