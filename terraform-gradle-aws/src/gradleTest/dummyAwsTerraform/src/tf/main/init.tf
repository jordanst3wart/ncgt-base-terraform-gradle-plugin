terraform {
  required_providers {
    environment = {
      source = "EppO/environment"
      version = "1.1.0"
    }
  }
}

data "environment_variables" "token" {
  filter = "^AWS_.+"
}

resource "null_resource" "token" {
  triggers = data.environment_variables.token.items
}