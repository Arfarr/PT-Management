# Keep kotlinx.serialization generated serializers
-keepclassmembers class com.ptmanager.data.model.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.ptmanager.data.model.**$$serializer { *; }
-keepclassmembers enum com.ptmanager.data.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}