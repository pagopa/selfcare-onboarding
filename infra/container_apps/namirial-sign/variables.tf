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
  })
}

variable "app_settings" {
  type = list(object({
    name  = string
    value = string
  }))
}

variable "secrets_names" {
  type        = map(string)
  description = "KeyVault secrets to get values from <env,secret-ref>"
}

variable "workload_profile_name" {
  type        = string
  description = "Workload Profile name to use"
  default     = null
}

variable "cae_name" {
  type        = string
  description = "Container App Environment name"
  default     = "cae-cp"
}

variable "suffix_increment" {
  type        = string
  description = "Suffix increment Container App Environment name"
  default     = ""
}

variable "ca_volume_mounts" {
  description = "List of volume mount for container app"
  type = list(object({
    mount_path  = string
    volume_name = string
  }))
  default = []
}

variable "enable_sws" {
  type = bool
  default = false
}
