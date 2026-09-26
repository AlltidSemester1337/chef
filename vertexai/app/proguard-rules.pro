# Gson deserializes recipe/nutrition data by reflection — keep field names and no-arg constructors.
-keepclassmembers class com.formulae.chef.feature.model.** {
    <fields>;
    <init>(...);
}
-keep class com.formulae.chef.feature.model.** { *; }

# Firebase-serialization intermediary classes (ChatHistoryRepositoryImpl) — same reflection needs.
-keepclassmembers class com.formulae.chef.services.persistence.Content { *; }
-keepclassmembers class com.formulae.chef.services.persistence.Part { *; }

# Gson's generic-signature-based TypeToken deserialization.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# Firebase Realtime Database deserializes POJOs by reflection using the same pattern as Gson.
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
}

# OpenTelemetry's optional Jackson-based JSON exporter and incubator metrics/trace/logs APIs are
# not used by this app (we export via OTLP/gRPC), so their absence at runtime is safe to ignore.
-dontwarn com.fasterxml.jackson.core.JsonFactory
-dontwarn com.fasterxml.jackson.core.JsonGenerator
-dontwarn io.opentelemetry.api.incubator.**

