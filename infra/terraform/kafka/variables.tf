variable "bootstrap_servers" {
  description = "Kafka bootstrap servers for Terraform provider."
  type        = list(string)
}

variable "topics" {
  description = "Kafka topics to manage."
  type = map(object({
    partitions         = number
    replication_factor = number
    config             = map(string)
  }))
}
