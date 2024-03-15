#This is to ensure I see bucket changes for Comments with commentsbot23
resource "aws_s3_bucket" "bamboo_checkov_bucket" {
  bucket = "bamboo-checkov-bucket"
}

resource "aws_s3_bucket_ownership_controls" "bamboo_checkov_bucket_ownership_controls" {
  bucket = aws_s3_bucket.bamboo_checkov_bucket.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}
#This is to ensure I see bucket changes for Hard fail
resource "aws_s3_bucket_public_access_block" "bamboo_checkov_bucket_public_access_block" {
  bucket                  = aws_s3_bucket.bamboo_checkov_bucket.id
  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_acl" "bamboo_checkov_bucket_acl" {
  depends_on = [
    aws_s3_bucket_ownership_controls.bamboo_checkov_bucket_ownership_controls,
    aws_s3_bucket_public_access_block.bamboo_checkov_bucket_public_access_block,
  ]

  bucket = aws_s3_bucket.bamboo_checkov_bucket.id
  acl    = "public-read"
}
