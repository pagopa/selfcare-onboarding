prefix    = "selc"
env       = "prod"
env_short = "p"
domain    = "onboarding-ms"
location  = "westeurope"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

ci_github_federations = [
  {
    repository = "selfcare-onboarding"
    subject    = "prod-ci"
  }
]

cd_github_federations = [
  {
    repository = "selfcare-onboarding"
    subject    = "prod-cd"
  }
]

environment_ci_roles = {
  subscription = [
    "Reader",
    "Key Vault Secrets User"
  ]
  resource_groups = {
    "terraform-state-rg" = [
      "Storage Blob Data Contributor"
    ]
  }
}

environment_cd_roles = {
  subscription = [
    "Reader",
    "Contributor"
  ]
  resource_groups = {
    "terraform-state-rg" = [
      "Storage Blob Data Contributor"
    ]
  }
}

github_repository_environment_ci = {
  protected_branches     = false
  custom_branch_policies = true
}

github_repository_environment_cd = {
  protected_branches     = true
  custom_branch_policies = false
  reviewers_teams        = ["selfcare-contributors"]
}
