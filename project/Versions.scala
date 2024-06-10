object Versions {
  val mysql             = "8.4.0"
  val flyway            = "10.12.0"
  val quill             = "4.8.4"
  val zio               = "2.1.1"
  val tapir             = "1.10.8"
  val pureConfig        = "0.17.6"
  val zioHttp           = "3.0.0-RC8"
  val zioTestContainers = "0.10.0"
  val chimney           = "1.0.0"
  val enumeratum =
    "1.7.0" // Note: Versions above 1.7.0 add Scala 3 support and are not binary compatible with previous versions
  val newType    = "0.4.4"
  val zioLogging = "2.3.0"
  val slf4j      = "1.7.36" // We keep version 1.7.36 because it's used by quill
  val circe      = "0.14.3"
}
