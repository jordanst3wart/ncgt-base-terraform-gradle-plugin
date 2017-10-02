provider "local" {
  version = "~> 1.0.0"
}

resource "local_file" "foo" {
  content     = "foo!"
  filename = "${path.module}/foo.bar"
}
