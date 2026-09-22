package com.tom.rv2ide.build.lightweight.steps

import com.tom.rv2ide.build.lightweight.BuildProgressListener
import com.tom.rv2ide.build.lightweight.BuildStep
import com.tom.rv2ide.build.lightweight.LightweightBuildConfig
import java.io.File

class ApkAligner : BuildStepExecutor() {
    override val step = BuildStep.ALIGN_APK
    override val stepName = "Aligning APK"

    override fun execute(config: LightweightBuildConfig, listener: BuildProgressListener): Boolean {
        listener.onStepStarted(step, stepName)
        
        val unalignedApk = File(config.outputDir, "app-unaligned.apk")
        val alignedApk = File(config.outputDir, "app-aligned.apk")
        
        if (!unalignedApk.exists()) {
            listener.onStepFailed(step, "Unaligned APK not found", null)
            return false
        }
        
        if (config.zipalign == null || !config.zipalign.exists()) {
            listener.onStepFailed(step, "Zipalign tool not found", null)
            return false
        }

        if (alignedApk.exists()) {
            alignedApk.delete()
        }

        val command = mutableListOf(
            config.zipalign.absolutePath,
            "-f",
            "4",
            unalignedApk.absolutePath,
            alignedApk.absolutePath
        )
        
        val success = runCommand(command, listener)

        if (success) {
            listener.onStepCompleted(step, "APK aligned successfully")
        } else {
            listener.onStepFailed(step, "APK alignment failed", null)
        }
        
        return success
    }
}
