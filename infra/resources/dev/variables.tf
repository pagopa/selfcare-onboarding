locals {
  prefix           = "selc"
  env_short        = "d"
  location         = westeurope
  suffix_increment = "-002"
  cae_name         = "cae-002"

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Dev"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare-onboarding"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

  cidr_subnet_document_storage = ["10.1.136.0/24"]

  key_vault = {
    resource_group_name = "selc-d-sec-rg"
    name                = "selc-d-kv"
  }

  project                  = "selc-${var.env_short}"
  ca_resource_group_name   = "${local.project}-container-app${var.suffix_increment}-rg"
  naming_config            = "documents"
  resource_group_name_vnet = "${var.project}-vnet-rg"
}

variable "app_name" {
  type        = string
  description = "App name"
}

variable "cidr_subnet_contract_storage" {
  type = list(string)
  description = "Documents storage address space."
}

variable "domain" {
  type        = string
  description = "Domain"
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

variable "instance_number" {
  type        = string
  description = "The istance number to create"
}

variable "is_pnpg" {
  type        = bool
  default     = false
  description = "(Optional) True if you want to apply changes to PNPG environment"
}

variable "location" {
  type    = string
  default = "westeurope"
}

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

variable "project" {
  type        = string
  description = "Selfcare prefix and short environment"
}

variable "subscription" {
  type = string
}

variable "suffix_increment" {
  type        = string
  description = "Suffix increment Container App Environment name"
  default     = ""
}

variable "resource_group_name" {
  type        = string
  description = "Name of the resource group where resources will be created"
}

variable "subnet_pep_id" {
  type = string
}

variable "tags" {
  type = map(any)
}
