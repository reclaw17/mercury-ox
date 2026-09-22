# R8/ProGuard rules for Mercury. Minification is enabled for release builds.
# Most libraries ship consumer rules in their AARs (Ktor, kotlinx.serialization,
# Tink), so this file only pins the reflection surfaces this app and the shared
# core rely on directly.

# --- kotlinx.serialization ---
# Generated serializers and companion serializer() lookups for this app's
# @Serializable models (gateway payloads, token store records).
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.unsupportedpastels.hermesandroid.**$$serializer { *; }
-keepclassmembers class com.unsupportedpastels.hermesandroid.** {
    *** Companion;
}
-keepclasseswithmembers class com.unsupportedpastels.hermesandroid.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Shared KMP core ---
# Protocol types, origin policy, and generated serializers must survive R8.
-keep class com.unsupportedpastels.mercury.core.** { *; }
-keep,includedescriptorclasses class com.unsupportedpastels.mercury.core.**$$serializer { *; }
-keepclassmembers class com.unsupportedpastels.mercury.core.** {
    *** Companion;
}

# --- Ktor (CIO engine) ---
# The client engine is discovered via ServiceLoader; keep the container so a
# minified build can still resolve it at runtime.
-keep class io.ktor.client.engine.cio.CIOEngineContainer { *; }
-keepclassmembers class io.ktor.** {
    volatile <fields>;
}

# --- Tink (AndroidKeystore AEAD token store) ---
# Tink registers key managers reflectively over protobuf-lite messages.
-keep class com.google.crypto.tink.proto.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# Kotlin coroutines ship their own consumer rules; nothing app-specific needed.
