provider "kafka" {
  bootstrap_servers = var.bootstrap_servers
  tls_enabled       = false
}

resource "kafka_topic" "topics" {
  for_each = var.topics

  name               = each.key
  partitions         = each.value.partitions
  replication_factor = each.value.replication_factor
  config             = each.value.config
}
