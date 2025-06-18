resource "azurerm_monitor_metric_alert" "iopstapi_throttling_low_availability" {
  name                = "[SELC | ${module.storage_api.name}] Low Availability"
  resource_group_name = var.resource_group_name
  scopes              = [module.storage_api.id]

  description   = "The average availability is less than 99.8%. Runbook: not needed."
  severity      = 0
  window_size   = "PT5M"
  frequency     = "PT5M"
  auto_mitigate = false

  # Metric info
  # https://learn.microsoft.com/en-us/azure/azure-monitor/essentials/metrics-supported#microsoftstoragestorageaccounts
  criteria {
    metric_namespace       = "Microsoft.Storage/storageAccounts"
    metric_name            = "Availability"
    aggregation            = "Average"
    operator               = "LessThan"
    threshold              = 99.8
    skip_metric_validation = false
  }

  action {
    action_group_id    = var.error_action_group_id
    webhook_properties = {}
  }

  tags = var.tags
}