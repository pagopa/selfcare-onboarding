prefix    = "selc"
env_short = "u"
env       = "uat"
location  = "westeurope"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

key_vault = {
  resource_group_name = "selc-u-sec-rg"
  name                = "selc-u-kv"
}
