prefix           = "selc"
env_short        = "p"
suffix_increment = "-002"
cae_name         = "cae-002"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_config = {
  cpu    = 2
  memory = 1
}

enable_sws = true
cidr_subnet_namirial_sws = ["10.1.150.0/29"]
environment_variables = {}
