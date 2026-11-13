-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Shizuku UserService and AIDL interfaces
-keep class com.xusu.sshlinkterminal.shizuku.** { *; }
-keep class * extends android.os.IInterface { *; }
-keepclassmembers class * extends android.os.Binder {
    public <methods>;
}

# Keep JSch and BouncyCastle cryptographic provider classes loaded via reflection
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**
-dontwarn org.ietf.jgss.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**
