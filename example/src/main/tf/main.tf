terraform {
  required_providers {
    local = {
      source = "hashicorp/local"
      # Setting the provider version is a strongly recommended practice
      # version = "..."
    }
  }
  # Provider functions require Terraform 1.8 and later.
  required_version = ">= 1.8.0"
}

# TODO change
terraform {
  backend "local" {}
}

resource "local_file" "foo" {
  content  = "foo!"
  filename = "${path.module}/foo.bar"
}

data "local_file" "foo" {
  filename = "${path.module}/foo.bar"
}

output "example_output" {
  value = provider::local::direxists("${path.module}/example-directory")
}