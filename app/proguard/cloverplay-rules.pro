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

# From: https://github.com/facebookarchive/conceal/blob/master/proguard_annotations.pro

-keep,allowobfuscation @interface com.facebook.crypto.proguard.annotations.DoNotStrip
-keep,allowobfuscation @interface com.facebook.crypto.proguard.annotations.KeepGettersAndSetters
-keep @com.facebook.crypto.proguard.annotations.DoNotStrip class *
-keepclassmembers class * {
    @com.facebook.crypto.proguard.annotations.DoNotStrip *;
}
-keepclassmembers @com.facebook.crypto.proguard.annotations.KeepGettersAndSetters class * {
  void set*(***);
  *** get*();
}
-keepclassmembers class * {
    com.facebook.jni.HybridData *;
    <init>(com.facebook.jni.HybridData);
}
-keepclasseswithmembers class * {
    com.facebook.jni.HybridData *;
}
-keep,allowobfuscation @interface com.facebook.proguard.annotations.DoNotStrip
-keep,allowobfuscation @interface com.facebook.proguard.annotations.KeepGettersAndSetters
-keep @com.facebook.proguard.annotations.DoNotStrip class *
-keepclassmembers class * {
    @com.facebook.proguard.annotations.DoNotStrip *;
}
-keepclassmembers @com.facebook.proguard.annotations.KeepGettersAndSetters class * {
  void set*(***);
  *** get*();
}

# From: https://github.com/topjohnwu/libsu/blob/master/core/proguard-rules.pro

-assumenosideeffects class com.topjohnwu.superuser.internal.InternalUtils {
  public static *** log(...);
}
-keep,allowobfuscation class * extends com.topjohnwu.superuser.Shell$Initializer

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
