locals {
  pnpg_suffix             = var.is_pnpg == true ? "-pnpg" : ""
  pnpg_domain_suffix      = var.is_pnpg == true ? "-weu-pnpg" : ""
  pnpg_domain_vnet_suffix = var.is_pnpg == true ? "-weu" : ""
  project                 = "${var.prefix}-${var.env_short}"
  env_url                 = var.env_short == "p" ? "https://api.selfcare.pagopa.it" :
    "https://api.${var.env}.selfcare.pagopa.it"

  app_name                 = "${local.project}${local.pnpg_suffix}-onboarding-fn"
  base_domain_name         = "${local.project}${local.pnpg_domain_suffix}"
  base_domain_vnet_name    = "${local.project}${local.pnpg_domain_vnet_suffix}"
  vnet_name                = "${local.project}-vnet-rg"
  monitor_rg_name          = "${local.project}-monitor-rg"
  monitor_appinsights_name = "${local.project}-appinsights"
}