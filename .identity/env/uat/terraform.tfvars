prefix    = "selc"
env       = "uat"
env_short = "u"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

user_registry_url = "https://api.uat.pdv.pagopa.it/user-registry/v1"


onboarding_allowed_institutions_products = "{\"prod-interop\":[\"*\"],\"prod-pn\":[\"*\"],\"prod-io\":[\"*\"],\"prod-io-premium\":[\"*\"],\"prod-pagopa\":[\"*\"],\"prod-fd\":[\"*\"],\"prod-fd-garantito\": [\"*\"] }"