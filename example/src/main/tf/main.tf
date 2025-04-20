provider "aws" {
  region = "ap-southeast-2"
}

# DynamoDB Table with on-demand capacity mode (pay-per-request)
resource "aws_dynamodb_table" "basic_table" {
  name           = "basic-table"
  billing_mode   = "PAY_PER_REQUEST"  # On-demand pricing, cost-effective for unpredictable workloads
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  tags = {
    Name        = "basic-dynamodb-table"
    Environment = "dev"
  }
}

# S3 Bucket with lifecycle policies for cost optimization
resource "aws_s3_bucket" "storage_bucket" {
  bucket = "my-cost-effective-bucket-${random_id.suffix.hex}"
}

# Adding lifecycle rules to transition objects to cheaper storage classes
resource "aws_s3_bucket_lifecycle_configuration" "bucket_lifecycle" {
  bucket = aws_s3_bucket.storage_bucket.id

  rule {
    id     = "transition-to-cheaper-storage"
    status = "Enabled"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"  # Cheaper storage for infrequently accessed data
    }

    transition {
      days          = 90
      storage_class = "GLACIER"  # Very low-cost for archival data
    }
  }
}

# Server-side encryption for S3 bucket
resource "aws_s3_bucket_server_side_encryption_configuration" "bucket_encryption" {
  bucket = aws_s3_bucket.storage_bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# CloudWatch Log Group with 30-day retention to control costs
resource "aws_cloudwatch_log_group" "app_logs" {
  name              = "/app/logs"
  retention_in_days = 30  # Set a reasonable retention to avoid unnecessary costs
}

# IAM Role with minimum necessary permissions
resource "aws_iam_role" "app_role" {
  name = "app-service-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

# Attach minimal policy to the role
resource "aws_iam_role_policy" "app_policy" {
  name = "app-policy"
  role = aws_iam_role.app_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:Query"
        ]
        Effect   = "Allow"
        Resource = aws_dynamodb_table.basic_table.arn
      },
      {
        Action = [
          "s3:GetObject",
          "s3:PutObject"
        ]
        Effect   = "Allow"
        Resource = "${aws_s3_bucket.storage_bucket.arn}/*"
      },
      {
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Effect   = "Allow"
        Resource = "${aws_cloudwatch_log_group.app_logs.arn}:*"
      }
    ]
  })
}

# Generate a random suffix for globally unique S3 bucket name
resource "random_id" "suffix" {
  byte_length = 4
}

# Outputs
output "dynamodb_table_name" {
  value = aws_dynamodb_table.basic_table.name
}

output "s3_bucket_name" {
  value = aws_s3_bucket.storage_bucket.bucket
}

output "cloudwatch_log_group_name" {
  value = aws_cloudwatch_log_group.app_logs.name
}

output "iam_role_arn" {
  value = aws_iam_role.app_role.arn
}
