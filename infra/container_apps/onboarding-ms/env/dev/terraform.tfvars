prefix    = "selc"
env_short = "d"

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
    value = "https://selc-d-onboarding-fn.azurewebsites.net"
  },
  {
    name  = "ONBOARDING_ALLOWED_INSTITUTIONS_PRODUCTS"
    value = "{'prod-interop': ['*'], 'prod-pn': ['*'], 'prod-io': ['*'], 'prod-io-premium': ['*'], 'prod-pagopa': ['*'], 'prod-fd': ['*'], 'prod-fd-garantito': ['*'], 'prod-sendino': ['*']}"
  },
  {
    name  = "STORAGE_CONTAINER_PRODUCT"
    value = "selc-d-product"
  },
  {
    name  = "MS_CORE_URL"
    value = "https://selc-d-ms-core-ca.gentleflower-c63e62fe.westeurope.azurecontainerapps.io"
  },
  {
    name  = "MS_PARTY_REGISTRY_URL"
    value = "https://selc-d-party-reg-proxy-ca.gentleflower-c63e62fe.westeurope.azurecontainerapps.io"
  },
  {
    name  = "SIGNATURE_VALIDATION_ENABLED"
    value = "false"
  }
]

secrets_names = {
  "JWT-PUBLIC-KEY"                          = "jwt-public-key"
  "MONGODB-CONNECTION-STRING"               = "mongodb-connection-string"
  "USER-REGISTRY-API-KEY"                   = "user-registry-api-key"
  "ONBOARDING-FUNCTIONS-API-KEY"            = "onboarding-functions-api-key"
  "BLOB-STORAGE-PRODUCT-CONNECTION-STRING"  = "blob-storage-product-connection-string"
  "BLOB-STORAGE-CONTRACT-CONNECTION-STRING" = "blob-storage-contract-connection-string"
}
