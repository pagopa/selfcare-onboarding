module "storage_account" {
  source  = "pagopa-dx/azure-storage-account/azurerm"
  version = "~>1.0"

  subnet_pep_id       = var.subnet_pep_id
  tags                = var.tags
  tier                = "l"
  environment         = local.environment
  resource_group_name = var.resource_group_name

  subservices_enabled = {
    blob  = true
    file  = false
    queue = false
    table = false
  }

  private_dns_zone_resource_group_name = var.private_dns_zone_resource_group_name
  network_rules = local.virtual_network

  blob_features = var.blob_features
}

# Lifecycle Management Policy
resource "azurerm_storage_management_policy" "lifecycle" {
  storage_account_id = module.storage_account.id

  # Regola per blob Hot -> Cool -> Archive -> Delete
  rule {
    name    = "lifecycle_rule_privacy"
    enabled = true

    filters {
      prefix_match = ["parties/deleted"]
      blob_types = ["blockBlob"]
    }

    actions {
      base_blob {
        tier_to_cool_after_days_since_modification_greater_than = var.base_blob_tier_to_cool_after_days_since_modification_greater_than
        tier_to_cold_after_days_since_creation_greater_than = var.base_blob_tier_to_cold_after_days_since_creation_greater_than
        delete_after_days_since_modification_greater_than = var.base_blobdelete_after_days_since_modification_greater_than
      }

      snapshot {
        change_tier_to_cool_after_days_since_creation = var.snapshot_change_tier_to_cool_after_days_since_creation
        delete_after_days_since_creation_greater_than          = var.snapshot_delete_after_days_since_creation_greater_than
      }

      version {
        change_tier_to_cool_after_days_since_creation = var.version_change_tier_to_cool_after_days_since_creation
        delete_after_days_since_creation          = var.version_delete_after_days_since_creation
      }
    }
  }
}