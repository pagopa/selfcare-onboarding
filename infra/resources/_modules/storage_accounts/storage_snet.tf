
resource "azurerm_resource_group" "documents_sa_rg" {
  name = "${local.project}-${local.naming_config}-storage-rg"
  //provider::dx::resource_name(merge(local.naming_config, { resource_type = "resource_group" }))
  location = var.location
}

resource "azurerm_subnet" "documents_snet" {
  name                 = "${local.project}-${local.naming_config}-snet"
  virtual_network_name = data.azurerm_virtual_network.vnet_selc.name
  resource_group_name  = data.azurerm_virtual_network.vnet_selc.resource_group_name
  address_prefixes     = var.cidr_subnet_contract_storage

  private_endpoint_network_policies = "Enabled"

  service_endpoints = [
    "Microsoft.Storage",
  ]
}
