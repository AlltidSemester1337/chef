# Firestore and Gson (de)serialize model classes by reflection — keep names, fields and no-arg
# constructors. Reflection-serialized classes live in feature.model, or carry @Keep (kept by the
# default Android rules). The verifyReleaseModelsKept Gradle task checks both after R8.
# Strip verbose/debug/info logging from release builds. Log.w/e stay for crash triage from
# logcat, so they must never carry user content (chat text, recipes, preferences, emails).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Gson deserializes recipe/nutrition data by reflection — keep field names and no-arg constructors.
-keepclassmembers class com.formulae.chef.feature.model.** {
    <fields>;
    <init>(...);
}
-keep class com.formulae.chef.feature.model.** { *; }

# Gson's generic-signature-based TypeToken deserialization.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# Firestore maps @PropertyName/@Exclude-annotated accessors by reflection.
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
}
