locals {
  prefix = "selc"

  mongo_db = {
    mongodb_rg_name               = "${local.prefix}-${var.env_short}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "${local.prefix}-${var.env_short}-cosmosdb-mongodb-account"
  }
}