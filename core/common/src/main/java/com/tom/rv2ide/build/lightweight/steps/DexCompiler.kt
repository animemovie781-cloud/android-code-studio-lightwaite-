package com.tom.rv2ide.build.lightweight.steps

import com.tom.rv2ide.build.lightweight.BuildProgressListener
import com.tom.rv2ide.build.lightweight.BuildStep
import com.tom.rv2ide.build.lightweight.LightweightBuildConfig
import com.tom.rv2ide.utils.Environment
import java.io.File

class DexCompiler : BuildStepExecutor() {
    override val step = BuildStep.DEX_CLASSES
    override val stepName = "Dexing classes"

    override fun execute(config: LightweightBuildConfig, listener: BuildProgressListener): Boolean {
        listener.onStepStarted(step, stepName)
        
        val classesDir = File(config.outputDir, "classes")
        val dexDir = File(config.outputDir, "dex")
        if (!dexDir.exists()) dexDir.mkdirs()
        
        if (!classesDir.exists() || classesDir.list()?.isEmpty() == true) {
            listener.onStepCompleted(step, "No classes to dex")
            return true
        }

        val command = mutableListOf(
            "dalvikvm",
            "-cp", config.d8Jar.absolutePath,
            "com.android.tools.r8.D8",
            "--output", dexDir.absolutePath,
            "--lib", config.androidJar.absolutePath
        )
        
        if (config.isRelease) {
            command.add("--release")
        } else {
            command.add("--debug")
        }
        
        if (config.minSdk > 0) {
            command.add("--min-api")
            command.add(config.minSdk.toString())
        }

        // Add input files (classes and lib jars)
        classesDir.walkTopDown().filter { it.isFile && it.name.endsWith(".class") }.forEach {
            command.add(it.absolutePath)
        }
        
        config.libJars.forEach {
            command.add(it.absolutePath)
        }
        
        val success = runCommand(command, listener)

        if (success) {
            listener.onStepCompleted(step, "Dexing completed successfully")
        } else {
            listener.onStepFailed(step, "Dexing failed", null)
        }
        
        return success
    }
}
