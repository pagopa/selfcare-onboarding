resource "github_repository_environment" "github_repository_environment_cd" {
  environment = "${var.env}-cd"
  repository  = local.github.repository

  dynamic "reviewers" {
    for_each = (var.github_repository_environment_cd.reviewers_teams == null ? [] : [1])
    content {
      teams = matchkeys(
        data.github_organization_teams.all.teams[*].id,
        data.github_organization_teams.all.teams[*].slug,
        var.github_repository_environment_cd.reviewers_teams
      )
    }
  }
  
  dynamic "deployment_branch_policy" {
    for_each = var.github_repository_environment_cd.protected_branches ? [1] : []
    content {
      protected_branches     = var.github_repository_environment_cd.protected_branches
      custom_branch_policies = var.github_repository_environment_cd.custom_branch_policies
    }
  }
}

resource "github_actions_environment_secret" "env_cd_secrets" {
  for_each        = local.env_cd_secrets
  repository      = local.github.repository
  environment     = github_repository_environment.github_repository_environment_cd.environment
  secret_name     = each.key
  plaintext_value = each.value
}

resource "github_actions_environment_variable" "env_cd_variables" {
  for_each      = local.env_cd_variables
  repository    = local.github.repository
  environment   = github_repository_environment.github_repository_environment_cd.environment
  variable_name = each.key
  value         = each.value
}