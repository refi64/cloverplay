-dontnote androidx.**
-dontwarn androidx.**
-dontnote com.google.android.material.**
-dontwarn com.google.android.material.**
-dontnote kotlin.**
-dontwarn kotlin.**
-dontwarn InnerClasses
-keepattributes SourceFile,LineNumberTable

# From: https://android.googlesource.com/platform/frameworks/support/+/refs/heads/pie-gsi/preference/proguard-rules.pro

-keep public class androidx.preference.Preference {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keep public class * extends androidx.preference.Preference {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# From: https://github.com/square/okhttp/blob/master/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro

# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.**

# From: https://github.com/topjohnwu/libsu/blob/master/core/proguard-rules.pro

-assumenosideeffects class com.topjohnwu.superuser.internal.Utils {
	public static void log(...);
	public static void ex(...);
}
-assumenosideeffects class com.topjohnwu.superuser.Shell.Config {
	public static void verboseLogging(...);
}
# TODO: Figure out why this isn't working (likely an old proguard version?)
#-assumevalues class com.topjohnwu.superuser.internal.Utils {
#	public static boolean vLog() return false;
#}
#-assumevalues class android.os.Debug {
#	public static boolean isDebuggerConnected() return false;
#}

-keep,allowobfuscation class * extends com.topjohnwu.superuser.Shell$Initializer { *; }
-keep,allowobfuscation class com.topjohnwu.superuser.ipc.IPCServer { *; }
-keep,allowobfuscation class * extends com.topjohnwu.superuser.ipc.RootService { *; }

# From: https://github.com/Kotlin/kotlinx.serialization#androidjvm

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.refi64.cloverplay.**$$serializer { *; }
-keepclassmembers class com.refi64.cloverplay.** {
    *** Companion;
}
-keepclasseswithmembers class com.refi64.cloverplay.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# From: https://github.com/material-components/material-components-android/blob/master/lib/proguard-behaviors.pro

-keep public class * extends androidx.coordinatorlayout.widget.CoordinatorLayout$Behavior {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>();
}
-keepattributes RuntimeVisible*Annotation*

# From: https://github.com/getsentry/sentry-android/blob/master/sentry-android-core/proguard-rules.pro

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class io.sentry.core.** { *; }
-keepclassmembers enum * { *; }
-keep class io.sentry.android.core.** { *; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
#-keep class * implements com.google.gson.TypeAdapter
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# don't warn jetbrains annotations
-dontwarn org.jetbrains.annotations.**

# R8: Attribute Signature requires InnerClasses attribute. Check -keepattributes directive.
-keepattributes InnerClasses
