package com.tom.rv2ide.build.lightweight

import java.io.File

enum class BuildStep {
    COMPILE_RESOURCES,
    LINK_RESOURCES,
    COMPILE_JAVA,
    DEX_CLASSES,
    PACKAGE_APK,
    ALIGN_APK,
    SIGN_APK
}

interface BuildProgressListener {
    fun onStepStarted(step: BuildStep, message: String)
    fun onStepCompleted(step: BuildStep, message: String)
    fun onStepFailed(step: BuildStep, error: String, details: String?)
    fun onOutput(line: String)
    fun onBuildCompleted(result: LightweightBuildResult)
}

data class LightweightBuildResult(
    val success: Boolean,
    val apkFile: File?,
    val errors: List<BuildError>,
    val warnings: List<String>,
    val buildTimeMs: Long
)

data class BuildError(
    val file: String?,
    val line: Int?,
    val column: Int?,
    val message: String,
    val step: BuildStep
)
