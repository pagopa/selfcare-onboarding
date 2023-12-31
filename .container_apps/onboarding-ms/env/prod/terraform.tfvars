prefix    = "selc"
env_short = "p"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_app = {
  min_replicas = 0
  max_replicas = 5
  scale_rules  = [
    {
      custom = {
        metadata = {
          "desiredReplicas" = "3"
          "start"           = "0 8 * * MON-FRI"
          "end"             = "0 19 * * MON-FRI"
          "timezone"        = "Europe/Rome"
        }
        type = "cron"
      }
      name = "cron-scale-rule"
    }
  ]
  cpu    = 1.25
  memory = "2.5Gi"
}

app_settings = [
  {
    name  = "USER_REGISTRY_URL"
    value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "ONBOARDING_FUNCTIONS_URL"
    value = "https://selc-p-onboarding-fn.azurewebsites.net"
  },
  {
    name  = "ONBOARDING_ALLOWED_INSTITUTIONS_PRODUCTS"
    value = "{'prod-interop': ['*'], 'prod-pn': ['*'], 'prod-io': ['*'], 'prod-io-premium': ['*'], 'prod-pagopa': ['*'], 'prod-fd': ['*'], 'prod-fd-garantito': ['*']}"
  },
  {
    name  = "STORAGE_CONTAINER_PRODUCT"
    value = "selc-p-product"
  },
  {
    name  = "MS_CORE_URL"
    value = "https://selc.internal.selfcare.pagopa.it/ms-core/v1"
  },
  {
    name  = "MS_PARTY_REGISTRY_URL"
    value = "http://selc.internal.selfcare.pagopa.it/party-registry-proxy/v1"
  }
]

key_vault = {
  resource_group_name = "selc-p-sec-rg"
  name                = "selc-p-kv"
  secrets_names = [
    "jwt-public-key",
    "mongodb-connection-string",
    "user-registry-api-key",
    "onboarding-functions-api-key",
    "start-completion-functions-api-key"
  ]
}
