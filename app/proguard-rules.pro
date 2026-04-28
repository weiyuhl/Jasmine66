# Repackage classes into a named package to avoid root-package conflicts with reflection.
-repackageclasses com.lhzkml.jasmine.repackaged

# Preserve annotations (required for Hilt, Serialization, JS bridge)
-keepattributes *Annotation*
-keepattributes JavascriptInterface

# Keep sandbox classes
-keep class com.android.sandbox.** { *; }
-keep class com.lhzkml.jasmine.feature.sandbox.** { *; }

# Keep prompt model, LLM, and executor classes (serialization/reflection)
-keep class com.lhzkml.jasmine.core.prompt.model.** { *; }
-keep class com.lhzkml.jasmine.core.prompt.llm.** { *; }
-keep class com.lhzkml.jasmine.core.prompt.executor.** { *; }

# Keep JavascriptInterface methods for WebView JS bridge
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends androidx.hilt.work.WorkerAssistedFactory { *; }

# Strip android.util.Log calls in release builds to prevent parameter leakage.
# FileLogger should be used for all in-app logging instead.
# Keep kotlinx-datetime classes used by Room converters and data models
-keep class kotlinx.datetime.** { *; }
-dontwarn kotlinx.datetime.Clock$System
-dontwarn kotlinx.datetime.Instant$Companion
-dontwarn kotlinx.datetime.Instant

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
