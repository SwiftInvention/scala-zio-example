object Versions {
  val mysql  = "9.7.0"
  val flyway = "12.5.0"
  val quill  = "4.8.5"
  val hikari =
    "6.2.1" // tracks the version quill-jdbc pulls transitively; libDb uses Hikari directly in DataSourceLayer
  val zio        = "2.1.25"
  val zioTest    = "2.1.25" // tracks zio core version
  val pureConfig = "0.17.10"
  val zioHttp    = "3.11.0"
  val zioJson    = "0.9.2"
  val zioSchema =
    "1.8.3" // version we get transitively via zio-http; pin explicitly so libCommon can use it without a server-framework dep
  val zioPrelude    = "1.0.0-RC47" // Note: prelude has been RC for years; widely used in production despite the label
  val enumeratum    = "1.9.7"
  val zioLogging    = "2.5.3"
  val slf4j         = "1.7.36"     // We keep version 1.7.36 because it's used by quill
  val zioTelemetry  = "3.1.16"     // ZIO 2.1.x compatible
  val openTelemetry = "1.61.0"     // zio-telemetry 3.1.16's transitive OpenTelemetry SDK version
  val openTelemetrySemconv = "1.40.0"
}
