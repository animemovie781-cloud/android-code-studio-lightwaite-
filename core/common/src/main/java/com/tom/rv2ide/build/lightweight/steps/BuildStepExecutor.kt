package com.tom.rv2ide.build.lightweight.steps

import com.tom.rv2ide.build.lightweight.BuildProgressListener
import com.tom.rv2ide.build.lightweight.LightweightBuildConfig
import java.io.File
import java.io.InputStreamReader
import java.io.BufferedReader
import com.tom.rv2ide.build.lightweight.BuildError
import com.tom.rv2ide.build.lightweight.BuildStep

abstract class BuildStepExecutor {
    abstract val step: BuildStep
    abstract val stepName: String
    
    abstract fun execute(config: LightweightBuildConfig, listener: BuildProgressListener): Boolean

    protected fun runCommand(command: List<String>, listener: BuildProgressListener): Boolean {
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                listener.onOutput(line ?: "")
            }

            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            listener.onOutput("Exception running command: ${e.message}")
            false
        }
    }
}
