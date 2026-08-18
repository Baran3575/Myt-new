# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.myt.player.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.myt.player.**$$serializer { *; }