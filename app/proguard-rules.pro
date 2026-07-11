# ──────────────────────────────────────────────
# R8 / ProGuard rules for APPDEX
# ──────────────────────────────────────────────

# ── General ──
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-dontwarn javax.annotation.**

# ── Kotlin Serialization ──
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.appdex.**$$serializer { *; }

# ── Coroutines ──
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }

# ── Hilt / Dagger ──
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.Hilt_AndroidApp { *; }
-keep,allowobfuscation @dagger.hilt.* class *
-keep,allowobfuscation @javax.inject.* class *

# ── Compose ──
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ── Room ──
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# ── Navigation (Type-safe) ──
-keep class * extends androidx.navigation.NavRoute { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }

# ── Apache Commons Net (FTP) ──
-dontwarn org.apache.commons.net.**
-keep class org.apache.commons.net.ftp.** { *; }

# ── ZXing ──
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ── Coil ──
-dontwarn coil.**

# ── Media3 / ExoPlayer ──
-dontwarn androidx.media3.**

# ── BouncyCastle ──
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ── Commons Compress ──
-dontwarn org.apache.commons.compress.**
-keep class org.apache.commons.compress.compressors.** { *; }

# ── Reflection-based code ──
-keep class com.appdex.data.** { *; }
-keep class com.appdex.model.** { *; }
-keep class com.appdex.plugin.** { *; }

# ── Native methods ──
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── Enum ──
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
