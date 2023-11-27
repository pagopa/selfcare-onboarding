data "azurerm_resource_group" "identity_rg" {
  name     = "${local.project}-identity-rg"
}

data "azurerm_user_assigned_identity" "identity_ci" {
  name                = "${local.project}-github-ci-identity"
  resource_group_name = data.azurerm_resource_group.identity_rg.name
}

data "azurerm_user_assigned_identity" "identity_cd" {
  name                = "${local.project}-github-cd-identity"
  resource_group_name = data.azurerm_resource_group.identity_rg.name
}

resource "github_repository_environment" "environment_ci" {
  environment = "${var.env}-ci"
  repository  = local.github.repository
}


resource "github_repository_environment" "environment_cd" {
  environment = "${var.env}-cd"
  repository  = local.github.repository

  # filter teams reviewers from github_organization_teams
  # if reviewers_teams is null no reviewers will be configured for environment
  dynamic "reviewers" {
    for_each = (var.github_repository_environment.reviewers_teams == null || var.env_short == "d" ? [] : [1])
    content {
      teams = matchkeys(
        data.github_organization_teams.all.teams.*.id,
        data.github_organization_teams.all.teams.*.name,
        var.github_repository_environment.reviewers_teams
      )
    }
  }
}

locals {
  env_secrets_ci = {
    "AZURE_CLIENT_ID_CI" : data.azurerm_user_assigned_identity.identity_ci.client_id,
    "AZURE_TENANT_ID" : data.azurerm_client_config.current.tenant_id,
    "AZURE_SUBSCRIPTION_ID" : data.azurerm_subscription.current.subscription_id
  }
  env_secrets_cd = {
    "AZURE_CLIENT_ID_CD" : data.azurerm_user_assigned_identity.identity_cd.client_id,
    "AZURE_TENANT_ID" : data.azurerm_client_config.current.tenant_id,
    "AZURE_SUBSCRIPTION_ID" : data.azurerm_subscription.current.subscription_id
  }
  env_variables = {
    "AZURE_ONBOARDING_FN_APP_NAME": "${local.project}-onboarding-fn",
    "AZURE_ONBOARDING_FN_RESOURCE_GROUP": "${local.project}-onboarding-fn-rg",
    "AZURE_ONBOARDING_FN_SERVICE_PLAN": "${local.project}-onboarding-fn-plan"
  }
  repo_secrets = {
    "SONAR_TOKEN": data.azurerm_key_vault_secret.sonar_token.value
  }
}

###############
# ENV Secrets #
###############

resource "github_actions_environment_secret" "github_environment_ci_secrets" {
  for_each        = local.env_secrets_ci
  repository      = local.github.repository
  environment     = github_repository_environment.environment_ci.environment
  secret_name     = each.key
  plaintext_value = each.value
}

resource "github_actions_environment_secret" "github_environment_cd_secrets" {
  for_each        = local.env_secrets_cd
  repository      = local.github.repository
  environment     = github_repository_environment.environment_cd.environment
  secret_name     = each.key
  plaintext_value = each.value
}

#################
# ENV Variables #
#################

resource "github_actions_environment_variable" "github_environment_cd_variables" {
  for_each      = local.env_variables
  repository    = local.github.repository
  environment     = github_repository_environment.environment_cd.environment
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
