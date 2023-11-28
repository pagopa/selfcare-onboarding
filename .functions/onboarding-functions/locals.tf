locals {
  project  = "${var.prefix}-${var.env_short}"
  app_name = "onboarding-functions"

  monitor_rg_name  = "${local.project}-monitor-rg"
  monitor_appinsights_name = "${local.project}-appinsights"
}