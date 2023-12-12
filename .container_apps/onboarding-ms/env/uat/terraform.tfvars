prefix    = "selc"
env_short = "u"

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
  cpu    = 0.5
  memory = "1Gi"
}

app_settings = [
  {
    name  = "USER_REGISTRY_URL"
    value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "ONBOARDING_FUNCTIONS_URL"
    value = "https://selc-u-onboarding-fn.azurewebsites.net"
  },
  {
    name  = "ONBOARDING_ALLOWED_INSTITUTIONS_PRODUCTS"
    value = "{'prod-interop': ['*'], 'prod-pn': ['*'], 'prod-io': ['*'], 'prod-io-premium': ['*'], 'prod-pagopa': ['*'], 'prod-fd': ['*'], 'prod-fd-garantito': ['*']}"
  },
  {
    name  = "STORAGE_CONTAINER_PRODUCT"
    value = "selc-u-product"
  },
  {
    name  = "MS_CORE_URL"
    value = "https://selc.internal.uat.selfcare.pagopa.it/ms-core/v1"
  },
  {
    name  = "MS_PARTY_REGISTRY_URL"
    value = "http://selc.internal.uat.selfcare.pagopa.it/party-registry-proxy/v1"
  },
  {
    name  = "SIGNATURE_VALIDATION_ENABLED"
    value = "true"
  }
]

key_vault = {
  resource_group_name = "selc-u-sec-rg"
  name                = "selc-u-kv"
  secrets_names = [
    "jwt-public-key",
    "mongodb-connection-string",
    "user-registry-api-key",
    "onboarding-functions-api-key",
    "blob-storage-product-connection-string",
    "blob-storage-contract-connection-string",
    "start-completion-functions-api-key"
  ]
}
