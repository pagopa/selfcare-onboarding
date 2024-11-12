resource "azurerm_container_group" "namirial_sws_cg" {

  count               = var.enable_sws ? 1 : 0
  name                = "${local.project}-namirial-sws-cg"
  location            = data.azurerm_resource_group.rg_contracts_storage.location
  resource_group_name = data.azurerm_resource_group.rg_contracts_storage.name
  ip_address_type     = "Public"
  dns_name_label      = "${local.project}-namirial-sws-cg"
  os_type             = "Linux"

  image_registry_credential {
    server   = "index.docker.io"
    username = data.azurerm_key_vault_secret.hub_docker_user.value
    password = data.azurerm_key_vault_secret.hub_docker_pwd.value
  }

  container {
    name   = "namirial-sws"
    image  = "namirial/sws:3.0.0"
    cpu    = var.container_config.cpu
    memory = var.container_config.memory

    ports {
      port     = 8080
      protocol = "TCP"
    }

    environment_variables = var.environment_variables

    readiness_probe {
      http_get {
        path   = "/SignEngineWeb/rest/ready"
        port   = 8080
        scheme = "Http"
      }
      initial_delay_seconds = 30
      timeout_seconds       = 4
    }

    liveness_probe {
      http_get {
        path   = "/SignEngineWeb/rest/ready"
        port   = 8080
        scheme = "Http"
      }
      initial_delay_seconds = 900
      timeout_seconds       = 4
    }

    volume {
      mount_path = "/opt/sws/custom"
      name       = "sws-storage"
      read_only  = false
      share_name = azurerm_storage_share.namirial_sws_storage_share[0].name

      storage_account_key  = azurerm_storage_account.namirial_sws_storage_account[0].primary_access_key
      storage_account_name = azurerm_storage_account.namirial_sws_storage_account[0].name
    }

  }

  diagnostics {
    log_analytics {
      workspace_id  = data.azurerm_log_analytics_workspace.log_analytics.workspace_id
      workspace_key = data.azurerm_log_analytics_workspace.log_analytics.primary_shared_key
    }
  }

  provisioner "local-exec" {
    command = <<EOT
      az storage file upload \
        --account-name ${azurerm_storage_account.namirial_sws_storage_account[0].name} \
        --account-key ${azurerm_storage_account.namirial_sws_storage_account[0].primary_access_key} \
        --share-name ${azurerm_storage_share.namirial_sws_storage_share[0].name} \
        --source "custom.properties" \
        --path "custom.properties" \
    EOT

    # Ensure it only runs on first creation
    when = create
  }

  tags = var.tags
}