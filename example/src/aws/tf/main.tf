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

provider "aws" {
  region = "us-east-1"
}

# TODO change
terraform {
  backend "local" {}
}

resource "local_file" "foo" {
  content  = "foo!"
  filename = "${path.module}/foo.txt"
}

// TODO replace with security group
resource "aws_cloudwatch_log_group" "example" {
  name = "example-log-group"

  tags = {
    Environment = "dev"
    Application = "serviceA"
  }
}
