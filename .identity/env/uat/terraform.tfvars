prefix    = "selc"
env       = "uat"
env_short = "u"
domain    = "onboarding-ms"
location  = "westeurope"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

ci_github_federations = [
  {
    repository = "selfcare-onboarding"
    subject    = "uat-ci"
  }
]

cd_github_federations = [
  {
    repository = "selfcare-onboarding"
    subject    = "uat-cd"
  }
]

environment_ci_roles = {
  subscription = ["Reader"]
  resource_groups = {
    "terraform-state-rg" = [
      "Storage Blob Data Reader"
    ]
  }
}

environment_cd_roles = {
  subscription = ["Reader"]
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
