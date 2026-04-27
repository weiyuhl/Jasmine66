# Repackage classes into the default package to reduce the size of descriptors.
-repackageclasses

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
