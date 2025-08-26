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

# Nostr-related rules
-keep class com.github.vitorpamplona.nostrcore.** { *; }
-dontwarn com.github.vitorpamplona.nostrcore.**

# Moshi JSON parsing
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Bouncy Castle cryptography
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep model classes that might be serialized/deserialized
-keep class com.github.therealcheebs.maintenancerecords.data.** { *; }
-keepclassmembers class com.github.therealcheebs.maintenancerecords.data.** { *; }

# Keep view binding classes
-keep class com.github.therealcheebs.maintenancerecords.databinding.** { *; }
-keepclassmembers class com.github.therealcheebs.maintenancerecords.databinding.** { *; }

# Keep AndroidX classes
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep Kotlin classes
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom view constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
