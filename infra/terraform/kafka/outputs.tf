output "managed_topic_names" {
  description = "Topic names managed by Terraform."
  value       = sort(keys(var.topics))
}

output "managed_topic_count" {
  description = "Total topic count managed by Terraform."
  value       = length(var.topics)
}
