# Add project specific ProGuard rules here.

# Room - Keep all entity and DAO classes
-keep class com.workout.timer.data.model.** { *; }
-keep class com.workout.timer.data.database.** { *; }

# Hilt - Keep generated classes
-keep class dagger.hilt.** { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Gson - Keep serialization
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Media3
-keep class androidx.media3.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Enum classes (for Room TypeConverters)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
