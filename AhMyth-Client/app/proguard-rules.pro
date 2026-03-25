# Optimize and Obfuscate Everything
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-repackageclasses ''
-allowaccessmodification

# Entry points (Activities, Services, Receivers) - KEEP but allow renaming of internal members
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# Keep essential Android members
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# Socket.io compatibility
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# Remove Log calls for better evasion
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Flatten packages
-flattenpackagehierarchy ''
-repackageclasses ''
