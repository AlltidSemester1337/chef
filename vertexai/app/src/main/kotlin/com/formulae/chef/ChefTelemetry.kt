package com.formulae.chef

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor

/**
 * Owns the process-wide OpenTelemetry [SdkTracerProvider] as a lazily-initialized singleton so
 * [OpenTelemetrySdk.buildAndRegisterGlobal] is only ever called once per process, even if
 * `MainActivity.onCreate` runs again (e.g. after a config change kills and recreates the Activity).
 */
object ChefTelemetry {
    val tracerProvider: SdkTracerProvider by lazy {
        val spanExporter = OtlpHttpSpanExporter.builder()
            .addHeader("Authorization", "Bearer ${BuildConfig.phoenixApiKey}")
            .addHeader("api_key", BuildConfig.phoenixApiKey)
            .setEndpoint("https://app.phoenix.arize.com/s/humlekottekonsult/v1/traces")
            .build()

        val resource = Resource.create(
            Attributes.builder()
                .put(AttributeKey.stringKey("service.name"), "Chef-Android")
                .put(AttributeKey.stringKey("project.name"), "Chef-Android")
                .put(AttributeKey.stringKey("openinference.project.name"), "Chef-Android")
                .build()
        )

        SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .setResource(resource)
            .build().also { provider ->
                OpenTelemetrySdk.builder()
                    .setTracerProvider(provider)
                    .buildAndRegisterGlobal()
            }
    }

    /** Force-exports any buffered spans. Call when the app leaves the foreground. */
    fun flush() {
        tracerProvider.forceFlush()
    }
}
