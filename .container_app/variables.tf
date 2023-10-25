variable "prefix" {
  description = "Domain prefix"
  type        = string
  default     = "selc"
  validation {
    condition = (
      length(var.prefix) <= 6
    )
    error_message = "Max length is 6 chars."
  }
}

variable "env_short" {
  description = "Environment short name"
  type        = string
  validation {
    condition = (
      length(var.env_short) <= 1
    )
    error_message = "Max length is 1 chars."
  }
}

variable "tags" {
  type = map(any)
}

variable "container_app" {
  description = "Container App configuration"
  type = object({
    image_tag    = string
    min_replicas = number
    max_replicas = number

    scale_rules = list(object({
      name = string
      custom = object({
        metadata = map(string)
        type     = string
      })
    }))

    cpu    = number
    memory = string

    app_settings = list(object({
      name  = string
      value = string
    }))
  })

  default = {
    image_tag    = "latest"
    min_replicas = 0
    max_replicas = 1

    scale_rules  = []
    app_settings = []
    env          = []

    cpu    = 0.5
    memory = "1Gi"
  }
}

variable "image_tag" {
  type = string
  default = "latest"
}

variable "key_vault" {
  description = "KeyVault data to get secrets values from"
  type = object({
    resource_group_name = string
    name                = string
    secrets_names       = set(string)
  })
}
