package com.formulae.chef.services.telemetry

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context

private const val INSTRUMENTATION_SCOPE = "com.formulae.chef"

fun getTracer(): Tracer = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_SCOPE)

/**
 * Runs [block] under a new "CHAIN"-kind span made current on the context, so any spans started
 * inside [block] via `tracer.spanBuilder(...).startSpan()` automatically parent to it (the OTel
 * SDK default when no explicit parent is set). Groups multi-step LLM call sequences into a single
 * trace instead of each call showing up as its own unlinked root span.
 */
suspend fun <T> withParentSpan(spanName: String, block: suspend () -> T): T {
    val span = getTracer().spanBuilder(spanName)
        .setAttribute("openinference.span.kind", "CHAIN")
        .startSpan()
    return try {
        Context.current().with(span).makeCurrent().use {
            block().also { span.setStatus(StatusCode.OK) }
        }
    } catch (e: Exception) {
        span.recordException(e)
        span.setStatus(StatusCode.ERROR)
        throw e
    } finally {
        span.end()
    }
}
