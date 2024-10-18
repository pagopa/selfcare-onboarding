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
    value = "-javaagent:applicationinsights-agent.jar",
  },
  {
    name  = "APPLICATIONINSIGHTS_ROLE_NAME"
    value = "onboarding-ms",
  },
  {
    name  = "USER_REGISTRY_URL"
    value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "ONBOARDING_FUNCTIONS_URL"
    value = "https://selc-u-pnpg-onboarding-fn.azurewebsites.net"
  },
  {
    name  = "ONBOARDING_ALLOWED_INSTITUTIONS_PRODUCTS"
    value = "{'prod-pn-pg': ['*']}"
  },
  {
    name  = "STORAGE_CONTAINER_PRODUCT"
    value = "selc-u-product"
  },
  {
    name  = "MS_CORE_URL"
    value = "http://selc-u-pnpg-ms-core-ca"
  },
  {
    name  = "MS_PARTY_REGISTRY_URL"
    value = "http://selc-u-pnpg-party-reg-proxy-ca"
  },
  {
    name  = "SIGNATURE_VALIDATION_ENABLED"
    value = "false"
  },
  {
    name  = "MS_USER_URL"
    value = "http://selc-u-pnpg-user-ms-ca"
  }
]

secrets_names = {
  "JWT-PUBLIC-KEY"                          = "jwt-public-key"
  "MONGODB-CONNECTION-STRING"               = "mongodb-connection-string"
  "USER-REGISTRY-API-KEY"                   = "user-registry-api-key"
  "ONBOARDING-FUNCTIONS-API-KEY"            = "fn-onboarding-primary-key"
  "BLOB-STORAGE-PRODUCT-CONNECTION-STRING"  = "blob-storage-product-connection-string"
  "BLOB-STORAGE-CONTRACT-CONNECTION-STRING" = "blob-storage-contract-connection-string"
  "APPLICATIONINSIGHTS_CONNECTION_STRING"   = "appinsights-connection-string"
}
