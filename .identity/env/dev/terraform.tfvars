prefix    = "selc"
env       = "dev"
env_short = "d"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

user_registry_url = "https://api.uat.pdv.pagopa.it/user-registry/v1"

environment_roles = {
  subscription = [
    "Reader",
    "Reader and Data Access",
    "Storage Blob Data Reader",
    "Storage File Data SMB Share Reader",
    "Storage Queue Data Reader",
    "Storage Table Data Reader",
    "PagoPA Export Deployments Template",
    "Key Vault Secrets User",
    "DocumentDB Account Contributor",
    "API Management Service Contributor",
  ]
}

onboarding_allowed_institutions_products = "{\"prod-interop\":[\"*\"],\"prod-pn\":[\"*\"],\"prod-io\":[\"*\"],\"prod-io-premium\":[\"*\"],\"prod-pagopa\":[\"*\"],\"prod-fd\":[\"*\"],\"prod-fd-garantito\": [\"*\"] }"

