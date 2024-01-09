resource "github_repository_environment" "github_repository_environment_ci" {
  environment = "${var.env}-ci"
  repository  = local.github.repository
  deployment_branch_policy {
    protected_branches     = var.github_repository_environment_ci.protected_branches
    custom_branch_policies = var.github_repository_environment_ci.custom_branch_policies
  }
}

resource "github_actions_environment_secret" "env_ci_secrets" {
  for_each        = local.env_ci_secrets
  repository      = local.github.repository
  environment     = github_repository_environment.github_repository_environment_ci.environment
  secret_name     = each.key
  plaintext_value = each.value
}
