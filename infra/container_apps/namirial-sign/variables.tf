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

variable "enable_sws" {
  type    = bool
  default = false
}

variable "environment_variables" {
  type        = map(string)
  description = "environment variables for container"
}

variable "container_config" {
  description = "Container configuration"
  type = object({
    cpu    = number
    memory = number
  })
}

variable "cidr_subnet_namirial_sws" {
  type        = list(string)
  description = "Cosmosdb pnpg address space."
  default     = ["10.1.154.0/29"]
}