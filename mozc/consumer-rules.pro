# mozcjni.cc looks this class up by its exact name and calls RegisterNatives on it, so R8 must not
# rename or strip it or any of the native method stubs.
-keep class com.google.android.apps.inputmethod.libs.mozc.session.MozcJNI { *; }

# protobuf-javalite reflects over generated message classes.
-keep class org.mozc.android.inputmethod.japanese.protobuf.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }
