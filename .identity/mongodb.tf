# Onboarding

resource "azurerm_cosmosdb_mongo_database" "selc_onboarding" {
  name                = "selcOnboarding"
  resource_group_name = local.mongo_db.mongodb_rg_name
  account_name        = local.mongo_db.cosmosdb_account_mongodb_name
}

resource "azurerm_management_lock" "mongodb_selc_onboarding" {
  name       = "mongodb-selc-onboarding-lock"
  scope      = azurerm_cosmosdb_mongo_database.selc_onboarding.id
  lock_level = "CanNotDelete"
  notes      = "This items can't be deleted in this subscription!"
}

module "mongdb_collection_onboardings" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//cosmosdb_mongodb_collection?ref=v7.3.0"

  name                = "onboardings"
  resource_group_name = local.mongo_db.mongodb_rg_name

  cosmosdb_mongo_account_name  = local.mongo_db.cosmosdb_account_mongodb_name
  cosmosdb_mongo_database_name = azurerm_cosmosdb_mongo_database.selc_onboarding.name

  indexes = [{
    keys   = ["_id"]
    unique = true
    }
  ]

  lock_enable = true
}

module "mongdb_collection_tokens" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//cosmosdb_mongodb_collection?ref=v7.3.0"

  name                = "tokens"
  resource_group_name = local.mongo_db.mongodb_rg_name

  cosmosdb_mongo_account_name  = local.mongo_db.cosmosdb_account_mongodb_name
  cosmosdb_mongo_database_name = azurerm_cosmosdb_mongo_database.selc_onboarding.name

  indexes = [{
    keys   = ["_id"]
    unique = true
    }
  ]

  lock_enable = true
}
