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
    name  = "USER_REGISTRY_URL"
    value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "ONBOARDING_FUNCTIONS_URL"
    value = "https://selc-d-pnpg-onboarding-fn.azurewebsites.net"
  },
  {
    name  = "ONBOARDING_ALLOWED_INSTITUTIONS_PRODUCTS"
    value = "{'prod-pn-pg': ['*']}"
  },
  {
    name  = "STORAGE_CONTAINER_PRODUCT"
    value = "selc-d-product"
  },
  {
    name  = "MS_CORE_URL"
    value = "https://dev01.pnpg.internal.dev.selfcare.pagopa.it/ms-core/v1"
  },
  {
    name  = "MS_PARTY_REGISTRY_URL"
    value = "http://dev01.pnpg.internal.dev.selfcare.pagopa.it/party-registry-proxy/v1"
  },
  {
    name  = "SIGNATURE_VALIDATION_ENABLED"
    value = "false"
  }
]

secrets_names = [
  "jwt-public-key",
  "mongodb-connection-string",
  "user-registry-api-key",
  "onboarding-functions-api-key",
  "blob-storage-product-connection-string",
  "blob-storage-contract-connection-string"
]
