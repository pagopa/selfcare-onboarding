locals {
  github = {
    org        = "pagopa"
    repository = "selfcare-onboarding"
  }

  prefix         = "selc"
  domain         = "onboarding"
  location_short = "weu"
  location       = "westeurope"
  project        = "${var.prefix}-${var.env_short}"

  app_name = "github-${local.github.org}-${local.github.repository}-${var.prefix}-${local.domain}-${var.env}"

  container_app_selc_environment = {
    name           = "${local.prefix}-${var.env_short}-container-app",
    resource_group = "${local.prefix}-${var.env_short}-container-app-rg",
  }

  functions = {
    resource_group_name = "${local.prefix}-${var.env_short}-functions-rg",
    insights_key = "${local.prefix}-${var.env_short}-appinsights"
  }

  mongo_db = {
    mongodb_rg_name = "${local.prefix}-${var.env_short}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "${local.prefix}-${var.env_short}-cosmosdb-mongodb-account"
  }
}

variable "env" {
  type = string
}

variable "env_short" {
  type = string
}

variable "user_registry_url" {
  type = string
}

variable "onboarding_functions_url" {
  type = string
}

variable "onboarding_allowed_institutions_products" {
  type = string
}

variable "prefix" {
  type    = string
  default = "selc"
  validation {
    condition = (
      length(var.prefix) <= 6
    )
    error_message = "Max length is 6 chars."
  }
}

variable "github_repository_environment" {
  type = object({
    protected_branches     = bool
    custom_branch_policies = bool
    reviewers_teams        = list(string)
  })
  description = "GitHub Continuous Integration roles"
  default = {
    protected_branches     = false
    custom_branch_policies = true
    reviewers_teams        = ["selfcare-team-admins"]
  }
}

variable "environment_roles" {
  type = object({
    subscription = list(string)
  })
  description = "GitHub Continous Integration roles"
}

variable "cosmosdb_mongodb_throughput" {
  type        = number
  description = "The throughput of the MongoDB database (RU/s). Must be set in increments of 100. The minimum value is 400. This must be set upon database creation otherwise it cannot be updated without a manual terraform destroy-apply."
  default     = 1000
}