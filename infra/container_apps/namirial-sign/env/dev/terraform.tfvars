prefix           = "selc"
env_short        = "d"
suffix_increment = "-002"
cae_name         = "cae-002"
location         = "westeurope"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_config = {
  cpu    = 0.5
  memory = 1
}

environment_variables = {
  SPRINGDOC_API_DOCS_ENABLED = true
}

enable_sws = true
