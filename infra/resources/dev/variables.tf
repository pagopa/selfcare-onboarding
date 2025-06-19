locals {
  prefix           = "sc"
  env_short        = "d"
  location         = "westeurope"
  suffix_increment = "-002"
  cae_name         = "cae-002"

  function_name    = "${local.project}-onboarding-fn"

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Dev"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare-onboarding"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

  cidr_subnet_document_storage = ["10.1.136.0/24"]

  key_vault = {
    resource_group_name = "selc-d-sec-rg"
    name                = "selc-d-kv"
  }

  project                  = "selc-${local.env_short}"
  ca_resource_group_name   = "${local.project}-container-app${local.suffix_increment}-rg"
  naming_config            = "documents"
  resource_group_name_vnet = "${local.project}-vnet-rg"

  cidr_subnet_contract_storage = ["10.1.136.0/24"]
}
