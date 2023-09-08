data "azurerm_storage_account" "tf_storage_account"{
  name                = "selc${var.env_short}stinfraterraform"
  resource_group_name = "io-infra-rg"
}

data "azurerm_resource_group" "dashboards" {
  name = "dashboards"
}

data "azurerm_kubernetes_cluster" "aks" {
  name                = local.aks_cluster.name
  resource_group_name = local.aks_cluster.resource_group_name
}

data "github_organization_teams" "all" {
  root_teams_only = true
  summary_only    = true
}

data "azurerm_key_vault" "key_vault" {
  name                = "${local.prefix}-${var.env_short}-kv"
  resource_group_name = "${local.prefix}-${var.env_short}-sec-rg"
}

data "azurerm_resource_group" "apim_resource_group" {
  name = "${local.product}-api-rg"
}
