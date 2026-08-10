# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Orbit 002: Room entities ---
-keep class com.orbit.app.data.entity.** { *; }
-keep class com.orbit.app.data.model.** { *; }

# --- Orbit 002: Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.orbit.app.**$$serializer { *; }
-keepclassmembers class com.orbit.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.orbit.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Orbit 002: AIDL Parcelables ---
-keep class com.orbit.app.data.ipc.** { *; }
-keep class com.orbit.app.net.ipc.** { *; }
# --- Orbit 003 (T101): AppFunctions + KSP-generated metadata ---
# AppFunctions discovers @AppFunction-annotated entry points reflectively
# via KSP-generated registry classes. R8 must not rename or strip them.
-keep @androidx.appfunctions.AppFunctionSchema class * { *; }
-keep @androidx.appfunctions.AppFunction class * { *; }
-keepclassmembers class * {
    @androidx.appfunctions.AppFunction <methods>;
}
# AppFunctions args data classes in :app — kept reflectively from JSON.
-keep class com.orbit.app.action.args.** { *; }
# AIDL parcelables added in 003 (re-affirmed; mirrors 002 rule).
-keep class com.orbit.app.action.ipc.** { *; }
