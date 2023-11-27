prefix    = "selc"
env_short = "u"
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


cidr_subnet_selc_onboarding_fn        = ["10.1.144.0/24"]

function_always_on = false

app_service_plan_info = {
  kind                         = "Linux"
  sku_size                     = "P1v3"
  sku_tier                     = "PremiumV3"
  maximum_elastic_worker_count = 1
  worker_count                 = 1
  zone_balancing_enabled       = false
}

storage_account_info = {
  account_kind                      = "StorageV2"
  account_tier                      = "Standard"
  account_replication_type          = "LRS"
  access_tier                       = "Hot"
  advanced_threat_protection_enable = false
}

app_settings = {
  USER_REGISTRY_URL = "https://api.uat.pdv.pagopa.it/user-registry/v1",
  ONBOARDING_FUNCTIONS_URL = "https://selc-d-func.azurewebsites.net"
}
