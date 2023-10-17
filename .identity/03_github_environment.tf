resource "github_repository_environment" "github_repository_environment" {
  environment = var.env
  repository  = local.github.repository
  # filter teams reviewers from github_organization_teams
  # if reviewers_teams is null no reviewers will be configured for environment
  dynamic "reviewers" {
    for_each = (var.github_repository_environment.reviewers_teams == null || var.env_short != "p" ? [] : [1])
    content {
      teams = matchkeys(
        data.github_organization_teams.all.teams.*.id,
        data.github_organization_teams.all.teams.*.name,
        var.github_repository_environment.reviewers_teams
      )
    }
  }
  deployment_branch_policy {
    protected_branches     = var.github_repository_environment.protected_branches
    custom_branch_policies = var.github_repository_environment.custom_branch_policies
  }
}

locals {
  env_secrets = {
    "CLIENT_ID" : azurerm_user_assigned_identity.environment.client_id,
    "TENANT_ID" : data.azurerm_client_config.current.tenant_id,
    "SUBSCRIPTION_ID" : data.azurerm_subscription.current.subscription_id,
    "FUNCTIONS_RESOURCE_GROUP": local.functions.resource_group_name,
    "SONAR_TOKEN": data.azurerm_key_vault_secret.sonar_token.value,
    "APP_INSIGHTS_KEY": local.functions.insights_key,
    "APP_REGION": local.location_short,
    "JWT_PUBLIC_KEY": base64encode(data.azurerm_key_vault_secret.jwt_public_key.value),
    "MONGODB_CONNECTION_URI": data.azurerm_key_vault_secret.mongodb_connection_string.value,
    "USER_REGISTRY_API_KEY": data.azurerm_key_vault_secret.user_registry_api_key.value,
    "ONBOARDING_FUNCTIONS_API_KEY": data.azurerm_key_vault_secret.onboarding_functions_api_key.value
  }
  env_variables = {
    "CONTAINER_APP_SELC_ENVIRONMENT_NAME" : local.container_app_selc_environment.name,
    "CONTAINER_APP_SELC_ENVIRONMENT_RESOURCE_GROUP_NAME" : local.container_app_selc_environment.resource_group,
    "USER_REGISTRY_URL" : var.user_registry_url,
    "ONBOARDING_FUNCTIONS_URL" : var.onboarding_functions_url,
    "ONBOARDING_ALLOWED_INSTITUTIONS_PRODUCTS": var.onboarding_allowed_institutions_products,
    "NAMESPACE" : local.domain,
  }
  repo_secrets = {
    # "SONAR_TOKEN" : data.azurerm_key_vault_secret.key_vault_sonar.value,
    # "BOT_TOKEN_GITHUB" : data.azurerm_key_vault_secret.key_vault_bot_token.value,
    # "CUCUMBER_PUBLISH_TOKEN" : data.azurerm_key_vault_secret.key_vault_cucumber_token.value,
  }
}

###############
# ENV Secrets #
###############

resource "github_actions_environment_secret" "github_environment_runner_secrets" {
  for_each        = local.env_secrets
  repository      = local.github.repository
  environment     = var.env
  secret_name     = each.key
  plaintext_value = each.value
}

#################
# ENV Variables #
#################


resource "github_actions_environment_variable" "github_environment_runner_variables" {
  for_each      = local.env_variables
  repository    = local.github.repository
  environment   = var.env
  variable_name = each.key
  value         = each.value
}

#############################
# Secrets of the Repository #
#############################


resource "github_actions_secret" "repo_secrets" {
  for_each        = local.repo_secrets
  repository      = local.github.repository
  secret_name     = each.key
  plaintext_value = each.value
}
