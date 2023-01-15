object Versions {
  val flyway     = "9.10.2"
  val mysql      = "8.0.31"
  val quill      = "3.19.0" // Note: Upgrading to 4.x brings ZIO 2.x
  val tapir      = "1.2.5"
  val zio        = "1.0.17" // Note: Upgrading to 2.x may break compatibility with internal libs
  val pureConfig = "0.17.2"
  val zioHttp           = "1.0.0.0-RC29" // Note: Upgrading to 2.x brings ZIO 2.x
  val zioTestContainers = "0.9.0"
  val chimney           = "0.6.2"
  val enumeratum =
    "1.7.0" // Note: Versions above 1.7.0 add Scala 3 support and are not binary compatible with previous versions
  val newType = "0.4.4"
}
