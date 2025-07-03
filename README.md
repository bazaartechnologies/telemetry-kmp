# Telemetry KMP 🚀

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blueviolet)](https://kotlinlang.org/docs/multiplatform.html)
[![OpenTelemetry](https://img.shields.io/badge/Observability-OpenTelemetry-informational)](https://opentelemetry.io/)

A modern, production-ready **Kotlin Multiplatform Telemetry SDK** for Android, iOS, and more. Unified tracing, metrics, and logging with full [OpenTelemetry](https://opentelemetry.io/) integration.

---

## Features

- **Kotlin Multiplatform**: Shared core for Android, iOS, and other targets
- **OpenTelemetry SDK**: Native integration for tracing, metrics, and logs (Android)
- **Custom OTLP/HTTP Exporter for iOS**: Uses Ktor for HTTP and Wire-generated types from official OpenTelemetry protos
- **Unified API**: Tracing, metrics, structured logging, and crash/vitals reporting
- **Pluggable Exporters**: OTLP/gRPC out of the box; extend for custom backends
- **App & Device Vitals**: Battery, memory, CPU, network, storage, and more
- **Lifecycle & Crash Handling**: Automatic app state and crash reporting
- **Easy Integration**: Use from Android, iOS (Swift/ObjC), or other KMP targets

---

## Current State

- **Android**: Uses the official OpenTelemetry Java SDK for full tracing, metrics, and logs via OTLP/gRPC.
- **iOS**: Implements a custom OTLP/HTTP exporter:
  - All telemetry data (spans, logs, metrics) is mapped to real Wire-generated types from the official OpenTelemetry proto files.
  - Data is exported over HTTP using Ktor.
  - Batching and retry logic are implemented.
  - The codebase currently generates gRPC client stubs via Wire, but these are not used for iOS/HTTP export and should be excluded in the future for leaner builds.

---

## Getting Started

### 1. Add Dependency

> **Coming soon:** Published artefacts for MavenCentral and CocoaPods/SwiftPM.

For now, build and publish locally:

```bash
./gradlew :telemetry-kmp:publishToMavenLocal
```

### 2. Initialize Telemetry

```kotlin
import com.bazaar.telemetry.*

Telemetry.init(
    DefaultTelemetryConfig(
        serviceName = "MyApp",
        endpoint = "https://otel.example.com"
    )
)
```

### 3. Usage Examples

#### Logging
```kotlin
Telemetry.log(LogLevel.INFO, "User logged in", attrs = Attributes.builder().put("user.id", "123").build())
```

#### Tracing
```kotlin
Telemetry.span("login") { span ->
    span.setAttribute("screen", "Login")
    // ...
}
```

#### Metrics
```kotlin
Telemetry.counter("api.requests", 1, attrs = Attributes.builder().put("endpoint", "/login").build())
Telemetry.gauge("battery.level", 87.0)
```

---

## Platform Support

- **Android**: Full OpenTelemetry SDK integration (tracing, metrics, logs via OTLP gRPC using official OpenTelemetry Java SDK).
- **iOS**: Telemetry export via a custom OTLP/HTTP exporter (traces, logs, metrics). This implementation uses Ktor for HTTP transport and requires manual OTLP protobuf message construction (via Wire-generated code from `.proto` schemas). All mapping now uses real proto types.
- **JVM/Other**: Extend as needed.

---

## Roadmap

1. **Exclude gRPC client code from Wire generation** for leaner builds and faster compilation (currently, unused gRPC stubs are generated).
2. **Add comprehensive tests and validation** for the iOS exporter and mapping logic.
3. **Monitor and migrate to the official OpenTelemetry KMP SDK for iOS** when it becomes stable and feature-complete.
4. **Add more exporters and platform support** as needed (e.g., Jaeger, Zipkin, custom backends).
5. **Improve documentation and usage examples** for all supported platforms.

---

## Contributing

Contributions, issues, and feature requests are welcome! Please open an issue or pull request.

---

## License

[Apache 2.0](LICENSE)
