prefix    = "selc"
env_short = "u"
location  = "westeurope"
is_pnpg   = true

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

key_vault = {
  resource_group_name = "selc-u-pnpg-sec-rg"
  name                = "selc-u-pnpg-kv"
}
