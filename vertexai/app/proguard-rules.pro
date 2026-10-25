# Firestore and Gson (de)serialize model classes by reflection — keep names, fields and no-arg
# constructors. Reflection-serialized classes live in feature.model, or carry @Keep (kept by the
# default Android rules). The verifyReleaseModelsKept Gradle task checks both after R8.
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
