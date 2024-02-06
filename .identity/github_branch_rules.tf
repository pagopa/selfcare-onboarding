resource "github_branch_default" "default_main" {
  repository = local.github.repository
  branch     = "main"
}

resource "github_branch_protection_v3" "protection_main" {
  repository = local.github.repository
  branch     = "main"

  required_pull_request_reviews {
    dismiss_stale_reviews           = false
    require_code_owner_reviews      = true
    required_approving_review_count = 1
  }

  required_status_checks {
    checks = []
  }
}

resource "github_branch_protection" "protection_releases" {
  repository_id = local.github.repository
  pattern       = "releases/*"

  require_conversation_resolution = true

  required_status_checks {
    strict = true
  }

  require_signed_commits = true

  required_pull_request_reviews {
    dismiss_stale_reviews           = true
    require_code_owner_reviews      = true
    required_approving_review_count = 1
    require_last_push_approval      = true

    pull_request_bypassers = [
      data.github_team.team_admins.node_id
    ]
  }

  allows_deletions = false
}

data "github_team" "team_admins" {
  slug = "selfcare-admin"
}