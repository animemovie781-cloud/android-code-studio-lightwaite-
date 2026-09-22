package com.tom.rv2ide.build.lightweight.steps

import com.tom.rv2ide.build.lightweight.BuildProgressListener
import com.tom.rv2ide.build.lightweight.BuildStep
import com.tom.rv2ide.build.lightweight.LightweightBuildConfig
import java.io.File

class ResourceLinker : BuildStepExecutor() {
    override val step = BuildStep.LINK_RESOURCES
    override val stepName = "Linking resources"

    override fun execute(config: LightweightBuildConfig, listener: BuildProgressListener): Boolean {
        listener.onStepStarted(step, stepName)
        
        val compiledDir = File(config.outputDir, "res-compiled")
        val genDir = File(config.outputDir, "gen")
        if (!genDir.exists()) genDir.mkdirs()
        
        val apkOut = File(config.outputDir, "resources.ap_")

        val command = mutableListOf(
            config.aapt2.absolutePath,
            "link",
            "-I", config.androidJar.absolutePath,
            "--manifest", config.manifestFile.absolutePath,
            "--java", genDir.absolutePath,
            "-o", apkOut.absolutePath,
            "--auto-add-overlay"
        )
        
        if (config.minSdk > 0) {
            command.add("--min-sdk-version")
            command.add(config.minSdk.toString())
        }
        
        if (config.targetSdk > 0) {
            command.add("--target-sdk-version")
            command.add(config.targetSdk.toString())
        }
        
        if (config.versionCode > 0) {
            command.add("--version-code")
            command.add(config.versionCode.toString())
        }
        
        if (config.versionName.isNotEmpty()) {
            command.add("--version-name")
            command.add(config.versionName)
        }

        // Add compiled res files
        if (compiledDir.exists()) {
            val compiledFiles = compiledDir.listFiles { _, name -> name.endsWith(".flat") }
            if (compiledFiles != null && compiledFiles.isNotEmpty()) {
                command.addAll(compiledFiles.map { it.absolutePath })
            }
        }

        val success = runCommand(command, listener)

        if (success) {
            listener.onStepCompleted(step, "Resources linked successfully")
        } else {
            listener.onStepFailed(step, "Resource linking failed", null)
        }
        
        return success
    }
}
