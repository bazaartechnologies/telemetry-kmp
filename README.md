# Telemetry KMP 🚀

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blueviolet)](https://kotlinlang.org/docs/multiplatform.html)
[![OpenTelemetry](https://img.shields.io/badge/Observability-OpenTelemetry-informational)](https://opentelemetry.io/)

A modern, production-ready **Kotlin Multiplatform Telemetry SDK** for Android, iOS, and more. Unified tracing, metrics, and logging with full [OpenTelemetry](https://opentelemetry.io/) integration.

---

## Features

- **Kotlin Multiplatform**: Shared core for Android, iOS, and other targets
- **OpenTelemetry SDK**: Native integration for tracing, metrics, and logs (Android)
- **Unified API**: Tracing, metrics, structured logging, and crash/vitals reporting
- **Pluggable Exporters**: OTLP/gRPC out of the box; extend for custom backends
- **App & Device Vitals**: Battery, memory, CPU, network, storage, and more
- **Lifecycle & Crash Handling**: Automatic app state and crash reporting
- **Easy Integration**: Use from Android, iOS (Swift/ObjC), or other KMP targets

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

- **Android**: Full OpenTelemetry SDK integration (tracing, metrics, logs via OTLP gRPC)
- **iOS**: Full OpenTelemetry SDK integration (tracing, metrics, logs via OTLP HTTP)
- **JVM/Other**: Extend as needed

---

## Contributing

Contributions, issues, and feature requests are welcome! Please open an issue or pull request.

---

## License

[Apache 2.0](LICENSE)
