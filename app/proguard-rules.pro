# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:\YiBin\eclipse\Android_SDK_windows/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class key to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontwarn org.mozilla.javascript.**
-dontwarn com.jecelyin.editor.**
-dontwarn com.makeramen.**
-dontwarn org.junit.**
-dontwarn junit.**
-dontwarn jackpal.androidterm.**
-dontwarn com.iwebpp.nodeandroid.**
-dontwarn org.msgpack.core.**
-dontwarn com.pushtorefresh.storio.**
-dontwarn java.lang.invoke.*
-dontwarn **$$Lambda$*

-keep class org.autojs.autojs.devplugin.message.** {*;}
-keep class org.mozilla.javascript.** { *; }
-keep class com.jecelyin.editor.** { *; }
-keep class com.stardust.automator.** { *; }
-keep class com.stardust.autojs.** { *; }
-keep class org.greenrobot.eventbus.** { *; }
-keep class * extends c
-keepattributes *Annotation*
# Event bus
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
# volley
-keepclassmembers class ** {
  @com.google.common.eventbus.Subscribe <methods>;
}
-keepclassmembers class ** {
  @com.some.package.server.JsonDeserializerWithOptions$FieldRequired public *;
}
-keep @interface com.some.package.server.JsonDeserializerWithOptions$FieldRequired
-keep class com.some.package.server.JsonDeserializerWithOptions
# autojs
-keepclassmembers class ** {
    @com.stardust.autojs.runtime.ScriptInterface <methods>;
}
# 920 editor
-keep class org.msgpack.** { *; }

# gson
-keep class * extends org.json.JSONObject {
    <fields>;
}

# JNI
-keepclasseswithmembernames class * {
    native <methods>;
}
# common
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}


-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepattributes Signature
-keepattributes EnclosingMethod


# Required to preserve the Flurry SDK
-keep class com.flurry.** { *; }
-dontwarn com.flurry.**
-keepattributes *Annotation*,EnclosingMethod,Signature

-keepclasseswithmembers class * {
	public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# AVLoadingView

-keep class com.wang.avi.** { *; }
-keep class com.wang.avi.indicators.** { *; }

# tencent

-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

-dontwarn dalvik.**

-keep class org.autojs.autoxjs.BuildConfig{
   *;
}

-keep interface kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader{
    *;
}
-keep class kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins.BuiltInsLoaderImpl{
    *;
}

# ===== 开启 R8 后由 AGP 生成的缺失类规则（missing_rules.txt）=====
# 均为可选/未打包的第三方依赖（netty native、log4j、conscrypt、jetty-npn、openjsse 等），
# 打 -dontwarn 即可。日后新增依赖时，重新跑 assembleV7Release 并对比此清单。
-dontwarn io.netty.internal.tcnative.AsyncSSLPrivateKeyMethod
-dontwarn io.netty.internal.tcnative.AsyncTask
-dontwarn io.netty.internal.tcnative.Buffer
-dontwarn io.netty.internal.tcnative.CertificateCallback
-dontwarn io.netty.internal.tcnative.CertificateCompressionAlgo
-dontwarn io.netty.internal.tcnative.CertificateVerifier
-dontwarn io.netty.internal.tcnative.Library
-dontwarn io.netty.internal.tcnative.SSL
-dontwarn io.netty.internal.tcnative.SSLContext
-dontwarn io.netty.internal.tcnative.SSLPrivateKeyMethod
-dontwarn io.netty.internal.tcnative.SSLSessionCache
-dontwarn io.netty.internal.tcnative.SessionTicketKey
-dontwarn io.netty.internal.tcnative.SniHostNameMatcher
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.apache.log4j.Level
-dontwarn org.apache.log4j.Logger
-dontwarn org.apache.log4j.Priority
-dontwarn org.apache.logging.log4j.Level
-dontwarn org.apache.logging.log4j.LogManager
-dontwarn org.apache.logging.log4j.Logger
-dontwarn org.apache.logging.log4j.message.MessageFactory
-dontwarn org.apache.logging.log4j.spi.ExtendedLogger
-dontwarn org.apache.logging.log4j.spi.ExtendedLoggerWrapper
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.BufferAllocator
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.conscrypt.HandshakeListener
-dontwarn org.eclipse.jetty.npn.NextProtoNego$ClientProvider
-dontwarn org.eclipse.jetty.npn.NextProtoNego$Provider
-dontwarn org.eclipse.jetty.npn.NextProtoNego$ServerProvider
-dontwarn org.eclipse.jetty.npn.NextProtoNego
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn reactor.blockhound.integration.BlockHoundIntegration
