# ProGuard / R8 keep rules for Zuzu Social AI

# Retrofit
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.Platform
-dontwarn retrofit2.Platform$Java8

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Moshi Codegen (@JsonClass(generateAdapter = true))
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keep,allowobfuscation,allowshrinking class * extends com.squareup.moshi.JsonAdapter
