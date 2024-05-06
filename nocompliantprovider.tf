/**
* File name may be "providers.tf", "provider.tf", "versions.tf", "version.tf". "main.tf" should also be scanned for provider declarations.
**/

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "5.45.0"
    }
    monkey = {
      source  = "aviatrixsystems/aviatrix"
      version = "3.1.4"
    }
  }

  cloud {
    # Backend configuration
  }
}
# All provider declarations will begin with "provider"
provider "aws" {
  region = var.region
}

provider "monkey" {}
