prefix    = "selc"
env_short = "d"
is_pnpg   = true

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_app = {
  min_replicas = 1
  max_replicas = 1
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
    value= "https://api-pnpg.dev.selfcare.pagopa.it/spid/v1"
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
    value =  "https://imprese.dev.notifichedigitali.it/auth/login/error"
   },
  {
    name  = "ENDPOINT_SUCCESS"
    value = "https://imprese.dev.notifichedigitali.it/auth/login/success"
  },
  {
    name  = " ENDPOINT_LOGIN"
    value    = "/login"
   },
  {
    name  = "ENDPOINT_METADATA"
    value = "/metadata"
   },
  {
    name  = "ENDPOINT_LOGOUT"
    value   = "/logout"
  },
  {
    name  = "SPID_ATTRIBUTES"
    value    = "name,familyName,fiscalNumber"
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
    value    = true
   },
  {
    name  = "COMPANY_EMAIL"
    value = "pagopa@pec.governo.it"
   },
  {
    name  = "COMPANY_FISCAL_CODE"
    value = 15376371009
   },
  {
    name  = "COMPANY_IPA_CODE"
    value = "PagoPA3"
   },
  {
    name  = "COMPANY_NAME"
    value = "PagoPA"
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
    value = "true  
   },
  {
    name  = "TOKEN_EXPIRATION"
    value = 3600
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
    value = "api-pnpg.dev.selfcare.pagopa.it"
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
    value = "selc-d-logs-blob"
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
    name  = "SPID_TESTENV_URL
    value = "https://selc-d-pnpg-spid-testenv.westeurope.azurecontainer.io"
  },
  {
    name  = "REDIS_PORT"
    value = "6380"
  },
  { 
    name  = "REDIS_URL"
    value = "selc-d-weu-pnpg-redis.redis.cache.windows.net
  }
]

secrets_names = {
  "SPID_LOGS_PUBLIC_KEY"                    = "spid-logs-encryption-public-key"
  "REDIS_PASSWORD"                          = "redis-primary-access-key"
  "APPLICATIONINSIGHTS_CONNECTION_STRING"   = "appinsights-connection-string"

  "JWT_TOKEN_PRIVATE_KEY"                   = "jwt-private-key"
  "JWT_TOKEN_KID"                           = "jwt-kid"
  "METADATA_PUBLIC_CERT"                    = "agid-spid-cert"
  "METADATA_PRIVATE_CERT"                   = "agid-spid-private-key"
  "USER_REGISTRY_API_KEY"                   = "user-registry-api-key"
  "SPID_LOGS_STORAGE_CONNECTION_STRING"     = "logs-storage-connection-string"
}
