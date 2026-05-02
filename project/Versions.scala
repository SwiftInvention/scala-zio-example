object Versions {
  val mysql             = "9.7.0"
  val flyway            = "12.5.0"
  val quill             = "4.8.5"
  val zio               = "2.1.25"
  val tapir             = "1.13.18"
  val pureConfig        = "0.17.10"
  val zioHttp           = "3.11.0"
  val zioTestContainers = "0.10.0" // Note: upstream appears abandoned (last release 2023)
  val chimney           = "1.10.0"
  val enumeratum        = "1.9.7"
  val newType           = "0.4.4"  // Note: upstream abandoned; consider replacing with opaque types or similar
  val zioLogging        = "2.5.3"
  val slf4j             = "1.7.36" // We keep version 1.7.36 because it's used by quill
  val circe             = "0.14.4" // Note: circe-generic-extras is deprecated; consider migrating
}
