###############################################################################
# Android Code Studio - R8/ProGuard Rules for Lightweight APK
# Optimized for maximum shrinking with R8 full mode
###############################################################################

# Note: -dontobfuscate is intentionally REMOVED to enable obfuscation
# This significantly reduces DEX size by shortening class/method names

-dontnote **

#==============================================================================
# JDK / OpenJDK / JAXP classes used by javac and language servers
#==============================================================================
-keep class javax.** { *; }
-keep class jdkx.** { *; }
-keep class openjdk.** { *; }
-keep class jaxp.** { *; }
-keep class org.w3c.** { *; }
-keep class org.xml.** { *; }

#==============================================================================
# Android builder model interfaces (used by Gradle tooling)
#==============================================================================
-keep class com.android.** { *; }

#==============================================================================
# Tooling API classes (loaded via JAR at runtime)
#==============================================================================
-keep class com.tom.rv2ide.tooling.** { *; }
-keep class com.tom.rv2ide.builder.model.** { *; }

#==============================================================================
# Eclipse / Lemminx (XML language server)
#==============================================================================
-keep class org.eclipse.** { *; }

#==============================================================================
# AutoService (service loader pattern)
#==============================================================================
-keep @com.google.auto.service.AutoService class ** {
}
-keepclassmembers class ** {
    @com.google.auto.service.AutoService <methods>;
}

#==============================================================================
# EventBus
#==============================================================================
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

#==============================================================================
# Reflectively accessed classes
#==============================================================================
-keep class io.github.rosemoe.sora.widget.component.EditorAutoCompletion {
    io.github.rosemoe.sora.widget.component.EditorCompletionAdapter adapter;
    int currentSelection;
}
-keep class com.tom.rv2ide.projects.util.StringSearch {
    packageName(java.nio.file.Path);
}
-keep class * implements org.antlr.v4.runtime.Lexer {
    <init>(...);
}
-keep class * extends com.tom.rv2ide.lsp.java.providers.completion.IJavaCompletionProvider {
    <init>(...);
}
-keep class com.tom.rv2ide.editor.api.IEditor { *; }
-keep class * extends com.tom.rv2ide.inflater.IViewAdapter { *; }
-keep class * extends com.tom.rv2ide.inflater.drawable.IDrawableParser {
    <init>(...);
    android.graphics.drawable.Drawable parse();
    android.graphics.drawable.Drawable parseDrawable();
}
-keep class com.tom.rv2ide.utils.DialogUtils { public <methods>; }

#==============================================================================
# APK Metadata (deserialized with GSON)
#==============================================================================
-keep class com.tom.rv2ide.models.ApkMetadata { *; }
-keep class com.tom.rv2ide.models.ArtifactType { *; }
-keep class com.tom.rv2ide.models.MetadataElement { *; }

#==============================================================================
# Parcelable
#==============================================================================
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

#==============================================================================
# Preferences (enums used reflectively)
#==============================================================================
-keep enum org.eclipse.lemminx.dom.builder.EmptyElements { *; }
-keep enum com.tom.rv2ide.xml.permissions.Permission { *; }

#==============================================================================
# Tree-sitter (JNI - native methods and fields accessed from native code)
#==============================================================================
-keepclasseswithmembers class ** {
    native <methods>;
}
-keep class com.tom.rv2ide.treesitter.** { *; }

#==============================================================================
# Retrofit 2
#==============================================================================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

#==============================================================================
# OkHttp3
#==============================================================================
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

#==============================================================================
# Stat uploader
#==============================================================================
-keep class com.tom.rv2ide.stats.** { *; }

#==============================================================================
# Gson
#==============================================================================
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

## Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

#==============================================================================
# Themes (enum used reflectively)
#==============================================================================
-keep enum com.tom.rv2ide.ui.themes.IDETheme {
  *;
}

#==============================================================================
# Contributor models - deserialized with GSON
#==============================================================================
-keep class * implements com.tom.rv2ide.contributors.Contributor {
  *;
}

#==============================================================================
# Kotlin Serialization (used in lsp-setup and other modules)
#==============================================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static ** Companion;
    <fields>;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

#==============================================================================
# Kotlin Coroutines
#==============================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

#==============================================================================
# Navigation SafeArgs (generated args classes)
#==============================================================================
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

#==============================================================================
# DataBinding
#==============================================================================
-keep class * extends androidx.databinding.DataBinderMapper { *; }
-keep class * extends androidx.databinding.ViewDataBinding {
    <init>(androidx.databinding.DataBindingComponent, android.view.View, int);
}

#==============================================================================
# Google Generative AI SDK
#==============================================================================
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

#==============================================================================
# SilentInstaller (Shizuku-based)
#==============================================================================
-keep class io.github.miyazkaori.silentinstaller.** { *; }

#==============================================================================
# Seasonal Effects
#==============================================================================
-keep class io.github.mohammedbaqernull.seasonal.** { *; }

#==============================================================================
# LogWire / Logger Service
#==============================================================================
-keep class io.github.mohammedbaqernull.logger.** { *; }

#==============================================================================
# Record classes (Java 16+ compatibility)
#==============================================================================
-keep class * extends java.lang.Record { *; }
-keepclassmembers class * extends java.lang.Record {
    <init>(...);
}

#==============================================================================
# Suppressed warnings (specific, not blanket)
#==============================================================================
## Annotation processing in Java Compiler
-dontwarn sun.reflect.annotation.AnnotationParser
-dontwarn sun.reflect.annotation.AnnotationType
-dontwarn sun.reflect.annotation.EnumConstantNotPresentExceptionProxy
-dontwarn sun.reflect.annotation.ExceptionProxy

## Logback
-dontwarn jakarta.servlet.ServletContainerInitializer

## JGit
-dontwarn java.lang.ProcessHandle
-dontwarn java.lang.management.ManagementFactory
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid

## Guava
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn afu.org.checkerframework.**

## SLF4J
-dontwarn org.slf4j.**

## Apache Commons
-dontwarn org.apache.commons.**

## Kotlin reflect (not used, suppress warnings)
-dontwarn kotlin.reflect.jvm.internal.**