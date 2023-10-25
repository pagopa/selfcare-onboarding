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
  image_tag    = "sha-384dd82"
  min_replicas = 0
  max_replicas = 1
  scale_rules  = []
  app_settings = [
    {
      name  = "USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "ONBOARDING_FUNCTIONS_URL"
      value = "https://selc-d-func.azurewebsites.net"
    },
    {
      name  = "ONBOARDING_ALLOWED_INSTITUTIONS_PRODUCTS"
      value = "{'prod-interop': ['*'], 'prod-pn': ['*'], 'prod-io': ['*'], 'prod-io-premium': ['*'], 'prod-pagopa': ['*'], 'prod-fd': ['*'], 'prod-fd-garantito': ['*']}"
    }
  ]
  cpu    = 0.5
  memory = "1Gi"
}

key_vault = {
  resource_group_name = "selc-d-sec-rg"
  name                = "selc-d-kv"
  secrets_names = [
    "jwt-public-key",
    "mongodb-connection-string",
    "user-registry-api-key",
    "onboarding-functions-api-key"
  ]
}
