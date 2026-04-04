# Repackage classes into the default package to reduce the size of descriptors.
-repackageclasses

# Keep sandbox classes
-keep class com.android.sandbox.** { *; }
-keep class com.lhzkml.jasmine.feature.sandbox.** { *; }

# Keep prompt model classes
-keep class com.lhzkml.jasmine.core.prompt.model.** { *; }
-keep class com.lhzkml.jasmine.core.prompt.llm.** { *; }
-keep class com.lhzkml.jasmine.core.prompt.executor.** { *; }

# Keep all jasmine classes for R8
-keep class com.lhzkml.jasmine.** { *; }

# Keep all jasmine classes for R8
-keep class com.lhzkml.jasmine.** { *; }

# Keep prompt model classes
-keep class com.lhzkml.jasmine.core.prompt.model.** { *; }
-keep class com.lhzkml.jasmine.core.prompt.llm.** { *; }
-keep class com.lhzkml.jasmine.core.prompt.executor.** { *; }

# Keep sandbox classes
-keep class com.android.sandbox.** { *; }
-keep class com.lhzkml.jasmine.feature.sandbox.** { *; }

# Keep proot loader classes
-keep class com.android.sandbox.core.** { *; }
