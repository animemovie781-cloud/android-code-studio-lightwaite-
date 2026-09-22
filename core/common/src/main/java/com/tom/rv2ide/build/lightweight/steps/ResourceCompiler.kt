package com.tom.rv2ide.build.lightweight.steps

import com.tom.rv2ide.build.lightweight.BuildProgressListener
import com.tom.rv2ide.build.lightweight.BuildStep
import com.tom.rv2ide.build.lightweight.LightweightBuildConfig
import java.io.File

class ResourceCompiler : BuildStepExecutor() {
    override val step = BuildStep.COMPILE_RESOURCES
    override val stepName = "Compiling resources"

    override fun execute(config: LightweightBuildConfig, listener: BuildProgressListener): Boolean {
        listener.onStepStarted(step, stepName)
        
        val compiledDir = File(config.outputDir, "res-compiled")
        if (!compiledDir.exists()) compiledDir.mkdirs()

        var success = true
        for (resDir in config.resDirs) {
            if (!resDir.exists()) continue
            
            val command = mutableListOf(
                config.aapt2.absolutePath,
                "compile",
                "--dir", resDir.absolutePath,
                "-o", compiledDir.absolutePath
            )
            
            if (!runCommand(command, listener)) {
                success = false
                break
            }
        }

        if (success) {
            listener.onStepCompleted(step, "Resources compiled successfully")
        } else {
            listener.onStepFailed(step, "Resource compilation failed", null)
        }
        
        return success
    }
}
