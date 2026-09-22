package com.tom.rv2ide.build.lightweight.steps

import com.tom.rv2ide.build.lightweight.BuildProgressListener
import com.tom.rv2ide.build.lightweight.BuildStep
import com.tom.rv2ide.build.lightweight.LightweightBuildConfig
import com.tom.rv2ide.utils.Environment
import java.io.File

class JavaCompiler : BuildStepExecutor() {
    override val step = BuildStep.COMPILE_JAVA
    override val stepName = "Compiling Java (ECJ)"

    override fun execute(config: LightweightBuildConfig, listener: BuildProgressListener): Boolean {
        listener.onStepStarted(step, stepName)
        
        val classesDir = File(config.outputDir, "classes")
        if (!classesDir.exists()) classesDir.mkdirs()
        
        val genDir = File(config.outputDir, "gen")
        val sourceFiles = mutableListOf<String>()
        
        if (genDir.exists()) {
            genDir.walkTopDown().filter { it.isFile && it.name.endsWith(".java") }.forEach {
                sourceFiles.add(it.absolutePath)
            }
        }
        
        config.javaSourceDirs.forEach { srcDir ->
            if (srcDir.exists()) {
                srcDir.walkTopDown().filter { it.isFile && it.name.endsWith(".java") }.forEach {
                    sourceFiles.add(it.absolutePath)
                }
            }
        }
        
        if (sourceFiles.isEmpty()) {
            listener.onStepCompleted(step, "No Java files to compile")
            return true
        }

        // ECJ (Eclipse Compiler for Java) requires android.jar and other libs
        val classpath = mutableListOf<String>()
        classpath.add(config.androidJar.absolutePath)
        config.libJars.forEach { classpath.add(it.absolutePath) }

        val sourceListFile = File(config.outputDir, "sources.txt")
        sourceListFile.writeText(sourceFiles.joinToString("\n"))

        val command = mutableListOf<String>()
        
        // Use dalvikvm to run ECJ directly on Android without JDK
        if (config.ecjJar != null && config.ecjJar.exists()) {
            command.addAll(listOf(
                "dalvikvm",
                "-cp", config.ecjJar.absolutePath,
                "org.eclipse.jdt.internal.compiler.batch.Main"
            ))
        } else {
            command.add("dalvikvm") // Fallback, though javac won't work on dalvikvm directly
        }

        command.addAll(listOf(
            "-d", classesDir.absolutePath,
            "-classpath", classpath.joinToString(File.pathSeparator),
            "-source", "1.8",
            "-target", "1.8"
        ))
        
        // ECJ uses slightly different syntax for source list sometimes, 
        // but @file works for both standard javac and ECJ.
        command.add("@${sourceListFile.absolutePath}")
        
        val success = runCommand(command, listener)

        if (success) {
            listener.onStepCompleted(step, "Java compiled successfully with ECJ")
        } else {
            listener.onStepFailed(step, "Java compilation failed", null)
        }
        
        return success
    }
}
