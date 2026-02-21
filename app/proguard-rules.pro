# Add project specific ProGuard rules here.

# DataStore — keep generated proto/preferences classes and serialization metadata
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Coil 3 — keep image loader and component registries
-keep class coil3.** { *; }
-dontwarn coil3.**

# Suppress Kotlin metadata warnings from R8
-dontwarn kotlin.Metadata
