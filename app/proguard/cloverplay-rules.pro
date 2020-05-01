-dontnote androidx.**
-dontwarn androidx.**
-dontnote com.google.android.material.**
-dontwarn com.google.android.material.**
-dontnote kotlin.**
-dontwarn kotlin.**
-dontwarn InnerClasses

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
