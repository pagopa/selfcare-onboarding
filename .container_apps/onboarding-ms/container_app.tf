data "azurerm_resource_group" "resource_group_app" {
  name = "${local.project}-container-app-rg"
}

data "azurerm_key_vault" "key_vault" {
  resource_group_name = var.key_vault.resource_group_name
  name                = var.key_vault.name
}

data "azurerm_key_vault_secrets" "key_vault_secrets" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "keyvault_secret" {
  for_each     = toset(data.azurerm_key_vault_secrets.key_vault_secrets.names)
  name         = each.key
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_container_app_environment" "container_app_environment" {
  resource_group_name = data.azurerm_resource_group.resource_group_app.name
  name                = "${local.project}-cae"
}

resource "azapi_resource" "container_app_onboarding_ms" {
  type      = "Microsoft.App/containerApps@2023-05-01"
  name      = "${local.project}-${local.app_name}-ca"
  location  = data.azurerm_resource_group.resource_group_app.location
  parent_id = data.azurerm_resource_group.resource_group_app.id

  tags = var.tags

  identity {
    type = "SystemAssigned"
  }

  body = jsonencode({
    properties = {
      configuration = {
        activeRevisionsMode = "Single"
        ingress = {
          allowInsecure = true
          external      = true
          traffic = [
            {
              latestRevision = true
              label          = "latest"
              weight         = 100
            }
          ]
          targetPort = 8080
        }
        secrets = local.secrets
      }
      environmentId = data.azurerm_container_app_environment.container_app_environment.id
      template = {
        containers = [
          {
            env   = concat(var.app_settings, local.secrets_env)
            image = "ghcr.io/pagopa/selfcare-onboarding-ms:${var.image_tag}"
            name  = "${local.project}-${local.app_name}"
            resources = {
              cpu    = var.container_app.cpu
              memory = var.container_app.memory
            }
          }
        ]
        scale = {
          maxReplicas = var.container_app.max_replicas
          minReplicas = var.container_app.min_replicas
          rules       = var.container_app.scale_rules
        }
      }
    }
  })
}

resource "azurerm_key_vault_access_policy" "keyvault_containerapp_access_policy" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azapi_resource.container_app_onboarding_ms.identity[0].principal_id

  secret_permissions = [
    "Get",
  ]
}

data "azurerm_resource_group" "rg_vnet" {
  name     = format("%s-vnet-rg", local.project)
}

data "azurerm_private_dns_zone" "private_azurecontainerapps_io" {
  name                = local.container_app_environment_dns_zone_name
  resource_group_name = data.azurerm_resource_group.rg_vnet.name
}

resource "azurerm_private_dns_a_record" "private_dns_record_a_azurecontainerapps_io" {
  name                = "${azapi_resource.container_app_onboarding_ms.name}.${trimsuffix(data.azurerm_container_app_environment.container_app_environment.default_domain, ".${local.container_app_environment_dns_zone_name}")}"
  zone_name           = data.azurerm_private_dns_zone.private_azurecontainerapps_io.name
  resource_group_name = data.azurerm_resource_group.rg_vnet.name
  ttl                 = 3600
  records             = [data.azurerm_container_app_environment.container_app_environment.static_ip_address]
}
