locals {
  pnpg_suffix = var.is_pnpg == true ? "-pnpg" : ""
  project     = "${var.prefix}-${var.env_short}"

  app_name                 = "${local.project}${local.pnpg_suffix}-onboarding-fn"
  vnet_name                = "${local.project}-vnet-rg"
  monitor_rg_name          = "${local.project}-monitor-rg"
  monitor_appinsights_name = "${local.project}-appinsights"
}