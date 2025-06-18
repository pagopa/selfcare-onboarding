output "storage_documents" {
  value = {
    id                  = module.storage_documents.id
    name                = module.storage_documents.name
    resource_group_name = module.storage_documents.resource_group_name
    primary_web_host    = module.storage_documents.primary_web_host
  }
}