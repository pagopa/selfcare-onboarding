module "container_app_onboarding_ms" {
  source = "github.com/pagopa/selfcare-commons//infra/terraform-modules/container_app_microservice?ref=main"

  is_pnpg = var.is_pnpg

  env_short                      = var.env_short
  container_app                  = var.container_app
  container_app_name             = "onboarding-ms"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-onboarding-ms"
  image_tag                      = var.image_tag
  app_settings                   = var.app_settings
  secrets_names                  = var.secrets_names
  workload_profile_name          = var.workload_profile_name

  probes = [
    {
      httpGet = {
        path   = "q/health/live"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds   = 5
      type             = "Liveness"
      failureThreshold = 3
    },
    {
      httpGet = {
        path   = "q/health/ready"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds   = 5
      type             = "Readiness"
      failureThreshold = 30
    },
    {
      httpGet = {
        path   = "q/health/started"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds   = 5
      failureThreshold = 5
      type             = "Startup"
    }
  ]

  tags = var.tags
}

