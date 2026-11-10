# ── Production ProGuard & R8 Optimization Rules ─────────────────────────────

# 1. Line Numbers & Stack Traces for Production Crash Reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 2. Strip Debug & Verbose Logging in Production
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# 3. Room Database - Keep Entities, DAOs, and TypeConverters
-keep class com.workout.timer.data.model.** { *; }
-keep class com.workout.timer.data.database.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

# 4. Hilt Dependency Injection
-keep class dagger.hilt.** { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# 5. Gson Serialization
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 6. Media3 (ExoPlayer)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# 7. Coil (Image & GIF Loader)
-keep class coil.** { *; }
-dontwarn coil.**

# 8. WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class androidx.work.WorkerParameters { *; }

# 9. AndroidX DataStore Preferences
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# 10. Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# 11. Enums (Room TypeConverters & Onboarding Enums)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
