package com.tom.rv2ide.build.lightweight.steps

import com.tom.rv2ide.build.lightweight.BuildProgressListener
import com.tom.rv2ide.build.lightweight.BuildStep
import com.tom.rv2ide.build.lightweight.LightweightBuildConfig
import com.tom.rv2ide.utils.Environment
import java.io.File

class ApkSigner : BuildStepExecutor() {
    override val step = BuildStep.SIGN_APK
    override val stepName = "Signing APK"

    override fun execute(config: LightweightBuildConfig, listener: BuildProgressListener): Boolean {
        listener.onStepStarted(step, stepName)
        
        val alignedApk = File(config.outputDir, "app-aligned.apk")
        val signedApk = File(config.outputDir, if (config.isRelease) "app-release.apk" else "app-debug.apk")
        
        if (!alignedApk.exists()) {
            listener.onStepFailed(step, "Aligned APK not found", null)
            return false
        }
        
        val keystore = config.debugKeystore
        if (keystore == null || !keystore.exists()) {
            listener.onStepFailed(step, "Keystore not found", null)
            return false
        }
        
        if (config.apksigner == null || !config.apksigner.exists()) {
            listener.onStepFailed(step, "Apksigner tool not found", null)
            return false
        }

        if (signedApk.exists()) {
            signedApk.delete()
        }

        val java = File(Environment.JAVA_HOME, "bin/java").absolutePath
        val command = mutableListOf(
            java,
            "-jar", config.apksigner.absolutePath,
            "sign",
            "--ks", keystore.absolutePath,
            "--ks-pass", "pass:android",
            "--key-pass", "pass:android",
            "--ks-key-alias", "androiddebugkey",
            "--in", alignedApk.absolutePath,
            "--out", signedApk.absolutePath
        )
        
        val success = runCommand(command, listener)

        if (success) {
            listener.onStepCompleted(step, "APK signed successfully: ${signedApk.name}")
        } else {
            listener.onStepFailed(step, "APK signing failed", null)
        }
        
        return success
    }
}
