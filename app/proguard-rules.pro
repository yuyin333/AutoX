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

# ===== Netty（Ktor 内嵌服务器 = DevPlugin 的「USB 调试」通道）=====
# Netty 是反射密集型库：Channel 由 io.netty.channel.ReflectiveChannelFactory 经
# Class.getConstructor() 反射创建；平台探测（PlatformDependent / Epoll / KQueue）同样大量用反射。
# R8 全量模式（AGP 默认）下，这类类往往只以 Class 字面量被引用 → 类体被判为不可达而整体剥离。
#
# 实测（2026-10-10，开启 R8 后暴露）：mapping.txt 里 io.netty.channel.socket.nio.NioServerSocketChannel
# 被剥成空壳（该类名下**一个成员都没有**，含无参构造器与 doReadMessages），连带 62 个 netty 类同样被剥空。
# 后果：抽屉 → Other → 「Turn on USB debugging」开关必然失败，弹
#   Service failed to start: Class a does not have a public non-arg constructor
# 堆栈：io.netty.channel.ReflectiveChannelFactory.<init>() → Class.getConstructor() →
#   NoSuchMethodException: E9.a.<init> []（消息里的 "a" 就是被混淆后的类简名）
#
# ⚠️ 规则范围要**收窄**：曾试过 -keep class io.netty.** { *; }（保留全部 2370 个 netty 类），
#   功能同样有效，但 R8 单次耗时从 ~2min 暴涨到 22min（APK 体积几乎不变，133.8MiB）——
#   因为把体积巨大的 io.netty.handler.* 编解码器也钉住不再裁剪。
#   因此只保留"Netty 自身用反射访问的核心包"：
#     io.netty.channel.*    ← ReflectiveChannelFactory 反射 new 出 Channel
#     io.netty.util.*       ← ResourceLeakDetectorFactory 按**方法名**找 toLeakAwareBuffer
#     io.netty.buffer.*     ← 同上，LeakAware Buffer 一族
#     io.netty.bootstrap.*  ← Bootstrap/FailedChannel 一族
#   这三处都是实测踩出来的：只留 channel.* 时启动会继续抛
#   NoClassDefFoundError(y9.k) ← ExceptionInInitializerError ←
#   IllegalArgumentException: Can't find '[toLeakAwareBuffer]' in y9.b
-keep class io.netty.channel.** { *; }
-keep class io.netty.util.** { *; }
-keep class io.netty.buffer.** { *; }
-keep class io.netty.bootstrap.** { *; }

# 泛型签名必须原样保留：Netty 的 TypeParameterMatcher 是**按类型参数名**在
# getGenericSuperclass() 链上查找的（MessageToMessageEncoder 构造器里找 "I"）。
# R8 会连签名里的类型参数名一起改写 → 实测连接一进来就抛：
#   Failed to initialize a channel. Closing: [id: 0x..., L:/127.0.0.1:9317 - R:...]
#   java.lang.IllegalStateException: unknown type parameter 'I': class x9.p
#     at io.netty.util.internal.TypeParameterMatcher.find0()
#     ← io.netty.handler.codec.MessageToMessageEncoder.<init>
#     ← HttpResponseEncoder ← HttpObjectEncoder ← HttpServerCodec
# 用 -keepnames（只锁名字，**不**阻止裁剪/优化）即可让签名不再被改写，
# 同时避免 -keep { *; } 带来的构建耗时暴涨。
-keepnames class io.netty.** { *; }
-keepattributes Signature
-dontwarn io.netty.**
