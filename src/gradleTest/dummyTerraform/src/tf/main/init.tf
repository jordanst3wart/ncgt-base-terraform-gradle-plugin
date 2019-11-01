variable "foofile" {
  type = string
}

resource "local_file" "foo" {
  content     = "foo"
  filename = var.foofile
}
