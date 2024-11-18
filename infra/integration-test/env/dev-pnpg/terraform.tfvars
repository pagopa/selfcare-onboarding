prefix    = "selc"
env_short = "d"
location  = "westeurope"
is_pnpg   = true

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

key_vault = {
  resource_group_name = "selc-d-pnpg-sec-rg"
  name                = "selc-d-pnpg-kv"
}
