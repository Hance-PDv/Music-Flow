-ignorewarnings
-dontwarn **
-dontnote **

# إيقاف مرحلة التحسين الثقيلة لمنع استهلاك الذاكرة وخطأ OutOfMemoryError
-dontoptimize

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { *; }
-keep class com.bumptech.glide.** { *; }

-dontwarn okhttp3.**
-dontnote okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

-repackageclasses ''
