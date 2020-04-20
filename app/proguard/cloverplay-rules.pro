-keep class com.google.protobuf.** { *; }
-keep public class com.refi64.cloverplay.Protos* {
  public protected *;
}

# ???
-keep public class com.google.gson.JsonElement
-keep public class kotlin.jvm.functions.Function1
-keep @interface kotlin.coroutines.** { *; }

-dontnote androidx.**
-dontnote com.google.android.material.**

# From: https://github.com/material-components/material-components-android/blob/master/lib/proguard-behaviors.pro

# CoordinatorLayout resolves the behaviors of its child components with reflection.
-keep public class * extends androidx.coordinatorlayout.widget.CoordinatorLayout$Behavior {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>();
}

# Make sure we keep annotations for CoordinatorLayout's DefaultBehavior
-keepattributes RuntimeVisible*Annotation*
