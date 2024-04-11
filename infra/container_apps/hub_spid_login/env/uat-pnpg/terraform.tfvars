prefix           = "selc"
env_short        = "u"
is_pnpg          = true
suffix_increment = "-001"
cae_name         = "cae-001"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_app = {
  min_replicas = 1
  max_replicas = 2
  scale_rules  = []
  cpu          = 0.5
  memory       = "1Gi"
}


app_settings = [
  {
    name  = "JAVA_TOOL_OPTIONS"
    value = "",
  },
  {
    name  = "APPLICATIONINSIGHTS_ROLE_NAME"
    value = "hub-spid-login-ms",
  },
  {
    name  = "ORG_URL"
    value = "https://www.pagopa.gov.it"
  },
  {
    name  = "ACS_BASE_URL"
    value = "https://api-pnpg.uat.selfcare.pagopa.it/spid/v1"
  },
  {
    name  = "ORG_DISPLAY_NAME"
    value = "PagoPA S.p.A"
  },
  {
    name  = "ORG_NAME"
    value = "PagoPA"
  },
  {
    name  = "AUTH_N_CONTEXT"
    value = "https://www.spid.gov.it/SpidL2"
  },
  {
    name  = "ENDPOINT_ACS"
    value = "/acs"
  },
  {
    name  = "ENDPOINT_ERROR"
    value = "https://imprese.uat.notifichedigitali.it/auth/login/error"
  },
  {
    name  = "ENDPOINT_SUCCESS"
    value = "https://imprese.uat.notifichedigitali.it/auth/login/success"
  },
  {
    name  = "ENDPOINT_LOGIN"
    value = "/login"
  },
  {
    name  = "ENDPOINT_METADATA"
    value = "/metadata"
  },
  {
    name  = "ENDPOINT_LOGOUT"
    value = "/logout"
  },
  {
    name  = "SPID_ATTRIBUTES"
    value = "name,familyName,fiscalNumber"
  },
  {
    name  = "SPID_VALIDATOR_URL"
    value = "https://validator.spid.gov.it"
  },
  {
    name  = "REQUIRED_ATTRIBUTES_SERVICE_NAME"
    value = "Portale Notifiche Digitali Imprese"
  },
  {
    name  = "ENABLE_FULL_OPERATOR_METADATA"
    value = "true"
  },
  {
    name  = "COMPANY_EMAIL"
    value = "pagopa@pec.governo.it"
  },
  {
    name  = "COMPANY_FISCAL_CODE"
    value = "15376371009"
  },
  {
    name  = "COMPANY_IPA_CODE"
    value = "PagoPA3"
  },
  {
    name  = "COMPANY_NAME"
    value = "PagoPA S.p.A."
  },
  {
    name  = "COMPANY_VAT_NUMBER"
    value = "IT15376371009"

  },
  {
    name  = "ENABLE_JWT"
    value = "true"
  },
  {
    name  = "INCLUDE_SPID_USER_ON_INTROSPECTION"
    value = "true"
  },
  {
    name  = "TOKEN_EXPIRATION"
    value = 32400
  },
  {
    name  = "JWT_TOKEN_ISSUER"
    value = "SPID"
  },
  {
    name  = "ENABLE_ADE_AA"
    value = "false"
  },
  {
    name  = "APPINSIGHTS_DISABLED"
    value = "false"
  },
  {
    name  = "ENABLE_USER_REGISTRY"
    value = "true"
  },
  {
    name  = "JWT_TOKEN_AUDIENCE"
    value = "api-pnpg.uat.selfcare.pagopa.it"
  },
  {
    name  = "ENABLE_SPID_ACCESS_LOGS"
    value = "true"
  },
  {
    name  = "SPID_LOGS_STORAGE_KIND"
    value = "azurestorage"
  },
  {
    name  = "SPID_LOGS_STORAGE_CONTAINER_NAME"
    value = "selc-u-logs-blob"
  },
  {
    name  = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL"
    value = "OFF"
  },
  {
    name  = "USER_REGISTRY_URL"
    value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "ORG_ISSUER"
    value = "https://selfcare.pagopa.it"
  },
  {
    name  = "CIE_URL"
    value = "https://preproduzione.idserver.servizicie.interno.gov.it/idp/shibboleth?Metadata"
  },
  {
    name  = "SERVER_PORT"
    value = "8080"
  },
  {
    name  = "IDP_METADATA_URL"
    value = "https://api.is.eng.pagopa.it/idp-keys/spid/latest"
  },
  {
    name  = "SPID_TESTENV_URL"
    value = "https://selc-u-pnpg-spid-testenv.westeurope.azurecontainer.io"
  },
  {
    name  = "REDIS_PORT"
    value = "6380"
  },
  {
    name  = "REDIS_URL"
    value = "selc-u-weu-pnpg-redis.redis.cache.windows.net"
  },
  {
    name  = "WELL_KNOWN_URL"
    value = "https://selcuweupnpgcheckoutsa.z6.web.core.windows.net/.well-known/jwks.json"
  }
]

secrets_names = {
  "SPID_LOGS_PUBLIC_KEY"                  = "spid-logs-encryption-public-key"
  "REDIS_PASSWORD"                        = "redis-primary-access-key"
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
  "APPINSIGHTS_INSTRUMENTATIONKEY"        = "appinsights-instrumentation-key"
  "JWT_TOKEN_PRIVATE_KEY"                 = "jwt-private-key"
  "JWT_TOKEN_KID"                         = "jwt-kid"
  "METADATA_PUBLIC_CERT"                  = "agid-spid-cert"
  "METADATA_PRIVATE_CERT"                 = "agid-spid-private-key"
  "USER_REGISTRY_API_KEY"                 = "user-registry-api-key"
  "SPID_LOGS_STORAGE_CONNECTION_STRING"   = "logs-storage-connection-string"
}
