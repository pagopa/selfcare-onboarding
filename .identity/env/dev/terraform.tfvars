prefix    = "selc"
env       = "dev"
env_short = "d"
domain    = "onboarding-ms"
location  = "westeurope"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

ci_github_federations = [
  {
    repository = "selfcare-onboarding"
    subject    = "dev-ci"
  }
]

cd_github_federations = [
  {
    repository = "selfcare-onboarding"
    subject    = "dev-cd"
  }
]

environment_ci_roles = {
  subscription = [
    "Reader",
    "PagoPA IaC Reader",
    "Reader and Data Access"
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
  custom_branch_policies = false
}

github_repository_environment_cd = {
  protected_branches     = false
  custom_branch_policies = false
}
