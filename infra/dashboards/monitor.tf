resource "azurerm_resource_group" "monitor_rg" {
  name     = "${local.project}-monitor-rg"
  location = var.location

  tags = var.tags
}

resource "azurerm_dashboard" "monitoring-dashboard" {
  name                = "${local.project}-monitoring-dashboard"
  resource_group_name = azurerm_resource_group.monitor_rg.name
  location            = azurerm_resource_group.monitor_rg.location
  tags                = var.tags

  dashboard_properties = templatefile("${path.module}/monitoring.json.tpl",
    {
      subscription_id = data.azurerm_subscription.current.subscription_id
      prefix          = "${var.prefix}-${var.env_short}"
    })
}