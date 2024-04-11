module "container_app_hub_spid_login" {
  source = "github.com/pagopa/selfcare-commons//infra/terraform-modules/container_app_microservice?ref=main"

  is_pnpg = var.is_pnpg

  env_short                      = var.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = var.container_app
  container_app_name             = "hub-spid-login"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "hub-spid-login-ms"
  image_tag                      = var.image_tag
  app_settings                   = var.app_settings
  secrets_names                  = var.secrets_names
  workload_profile_name          = var.workload_profile_name
  probes = [
    {
      httpGet = {
        path   = "/info"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds   = 5
      type             = "Liveness"
      failureThreshold = 5
    },
    {
      httpGet = {
        path   = "/info"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds   = 5
      type             = "Readiness"
      failureThreshold = 3
    },
    {
      httpGet = {
        path   = "/info"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds   = 5
      failureThreshold = 30
      type             = "Startup"
    }
  ]

  tags = var.tags
}

