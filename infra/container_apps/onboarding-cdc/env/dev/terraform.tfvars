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
  min_replicas = 0
  max_replicas = 1
  scale_rules = [
    {
      custom = {
        metadata = {
          "desiredReplicas" = "1"
          "start"           = "0 8 * * MON-FRI"
          "end"             = "0 19 * * MON-FRI"
          "timezone"        = "Europe/Rome"
        }
        type = "cron"
      }
      name = "cron-scale-rule"
    }
  ]
  cpu    = 1
  memory = "2Gi"
}

app_settings = [
  {
    name  = "JAVA_TOOL_OPTIONS"
    value = "-javaagent:applicationinsights-agent.jar",
  },
  {
    name  = "APPLICATIONINSIGHTS_ROLE_NAME"
    value = "onboarding-cdc",
  },
  {
    name  = "ONBOARDING_FUNCTIONS_URL"
    value = "https://selc-d-onboarding-fn.azurewebsites.net"
  },
  {
    name  = "ONBOARDING-CDC-MONGODB-WATCH-ENABLED"
    value = "true"
  }
]


secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
  "MONGODB-CONNECTION-STRING"             = "mongodb-connection-string"
  "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
}

