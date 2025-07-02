# Telemetry KMP 🚀

Minimal Kotlin Multiplatform rewrite of the original Android‑only telemetry library.

* **Platforms**: Android + iOS (Kotlin/Native)
* **Shared Core**: Tracing, metrics, structured logging API lives in `commonMain`
* **Platform Layers**: Android uses OpenTelemetry SDK, iOS prints/logs for now (hook up OTLP exporter next)

## Quick start

```kotlin
Telemetry.init(
    DefaultTelemetryConfig(
        serviceName = "MyApp",
        endpoint = "https://otel.example.com"
    )
)

Telemetry.log(LogLevel.INFO, "hello world")

Telemetry.span("login") {
    setAttribute("screen", "Login")
}
```

## Building

```bash
./gradlew :telemetry-kmp:publishToMavenLocal
```

The artefact can be consumed from Android and Swift (via CocoaPods or SwiftPM).
