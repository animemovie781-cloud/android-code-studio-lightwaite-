package com.tom.rv2ide.build.lightweight

import com.tom.rv2ide.build.lightweight.steps.*
import java.io.File

class LightweightBuildEngine(
    private val config: LightweightBuildConfig,
    private val listener: BuildProgressListener
) {
    private val steps = listOf(
        ResourceCompiler(),
        ResourceLinker(),
        JavaCompiler(),
        DexCompiler(),
        ApkPackager(),
        ApkAligner(),
        ApkSigner()
    )

    fun build(): LightweightBuildResult {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<BuildError>()
        
        // Ensure output dir exists
        if (!config.outputDir.exists()) {
            config.outputDir.mkdirs()
        }

        var success = true
        for (step in steps) {
            try {
                if (!step.execute(config, listener)) {
                    success = false
                    errors.add(BuildError(null, null, null, "Step failed: ${step.stepName}", step.step))
                    break
                }
            } catch (e: Exception) {
                success = false
                errors.add(BuildError(null, null, null, "Exception in step ${step.stepName}: ${e.message}", step.step))
                listener.onStepFailed(step.step, "Exception occurred", e.message)
                break
            }
        }

        val apkFile = if (success) {
            File(config.outputDir, if (config.isRelease) "app-release.apk" else "app-debug.apk")
        } else {
            null
        }
        
        val result = LightweightBuildResult(
            success = success,
            apkFile = apkFile,
            errors = errors,
            warnings = emptyList(),
            buildTimeMs = System.currentTimeMillis() - startTime
        )
        
        listener.onBuildCompleted(result)
        return result
    }
}
