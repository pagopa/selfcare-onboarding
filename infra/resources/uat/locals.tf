locals {
  prefix    = "sc"
  env_short = "u"
  location  = "westeurope"
  # suffix_increment = "-002"

  function_name = "${local.project}-onboarding-fn"

  tags = {
    CreatedBy   = "Terraform"
    Environment = "UAT"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare-onboarding"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

  cidr_subnet_document_storage = ["10.1.136.0/24"]

  key_vault_resource_group_name = "selc-u-sec-rg"
  key_vault_name                = "selc-u-kv"

  project                  = "selc-${local.env_short}"
  naming_config            = "documents"
  resource_group_name_vnet = "${local.project}-vnet-rg"

  cidr_subnet_contract_storage = ["10.1.136.0/24"]
}
