variable "env" {
  type = string
}

variable "env_short" {
  type = string
}

variable "domain" {
  type = string
}

variable "location" {
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

variable "tags" {
  type = map(any)
}


variable "ci_github_federations" {
  type = list(object({
    repository        = string
    credentials_scope = optional(string, "environment")
    subject           = string
  }))
  description = "GitHub Organization, repository name and scope permissions"
}

variable "cd_github_federations" {
  type = list(object({
    repository        = string
    credentials_scope = optional(string, "environment")
    subject           = string
  }))
  description = "GitHub Organization, repository name and scope permissions"
}

variable "environment_ci_roles" {
  type = object({
    subscription    = list(string)
    resource_groups = map(list(string))
  })
  description = "Continous Integration roles for managed identity"
}

variable "environment_cd_roles" {
  type = object({
    subscription    = list(string)
    resource_groups = map(list(string))
  })
  description = "Continous Delivery roles for managed identity"
}

variable "github_repository_environment_ci" {
  type = object({
    protected_branches     = bool
    custom_branch_policies = bool
    reviewers_teams        = optional(list(string), [])
    branch_pattern         = optional(string, null)
  })
  description = "GitHub Continous Integration roles"
}

variable "github_repository_environment_cd" {
  type = object({
    protected_branches     = bool
    custom_branch_policies = bool
    reviewers_teams        = optional(list(string), [])
    branch_pattern         = optional(string, null)
  })
  description = "GitHub Continous Delivery roles"
}
