package com.tom.rv2ide.build.lightweight

import java.io.File

data class LightweightBuildConfig(
    val projectDir: File,
    val outputDir: File,
    val androidJar: File,
    val aapt2: File,
    val d8Jar: File,
    val ecjJar: File?,
    val zipalign: File?,
    val apksigner: File?,
    val debugKeystore: File?,
    val applicationId: String,
    val minSdk: Int = 21,
    val targetSdk: Int = 34,
    val versionCode: Int = 1,
    val versionName: String = "1.0",
    val isRelease: Boolean = false,
    val javaSourceDirs: List<File>,
    val resDirs: List<File>,
    val manifestFile: File,
    val libJars: List<File> = emptyList()
)
