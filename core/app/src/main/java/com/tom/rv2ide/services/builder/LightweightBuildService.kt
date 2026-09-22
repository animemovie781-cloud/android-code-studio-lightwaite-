package com.tom.rv2ide.services.builder

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.tom.rv2ide.build.lightweight.*
import com.tom.rv2ide.projects.builder.BuildService
import com.tom.rv2ide.tooling.api.messages.InitializeProjectParams
import com.tom.rv2ide.tooling.api.messages.result.BuildCancellationRequestResult
import com.tom.rv2ide.tooling.api.messages.result.InitializeResult
import com.tom.rv2ide.tooling.api.messages.result.TaskExecutionResult
import com.tom.rv2ide.tooling.api.models.ToolingServerMetadata
import com.tom.rv2ide.utils.Environment
import java.io.File
import java.util.concurrent.CompletableFuture

class LightweightBuildService : Service(), BuildService {
    override val isBuildInProgress: Boolean = false

    override fun isToolingServerStarted(): Boolean = true

    override fun metadata(): CompletableFuture<ToolingServerMetadata> {
        return CompletableFuture.completedFuture(
            ToolingServerMetadata(android.os.Process.myPid())
        )
    }

    override fun initializeProject(params: InitializeProjectParams): CompletableFuture<InitializeResult> {
        return CompletableFuture.completedFuture(null)
    }

    override fun executeTasks(vararg tasks: String): CompletableFuture<TaskExecutionResult> {
        return CompletableFuture.supplyAsync {
            // Task format is "projectDir:taskName"
            val projectDirStr = tasks.firstOrNull()?.substringBefore(":") ?: return@supplyAsync null
            val projectDir = File(projectDirStr)
            val projectConfig = LightweightProjectConfig.fromFile(File(projectDir, "project.json"))
                ?: return@supplyAsync null

            val buildConfig = LightweightBuildConfig(
                projectDir = projectDir,
                outputDir = File(projectDir, "build"),
                androidJar = Environment.ANDROID_JAR,
                aapt2 = Environment.AAPT2,
                d8Jar = Environment.D8_JAR,
                ecjJar = Environment.ECJ_JAR,
                zipalign = Environment.ZIPALIGN,
                apksigner = Environment.APKSIGNER_JAR,
                debugKeystore = Environment.DEBUG_KEYSTORE,
                applicationId = projectConfig.applicationId,
                minSdk = projectConfig.minSdk,
                targetSdk = projectConfig.targetSdk,
                versionCode = projectConfig.versionCode,
                versionName = projectConfig.versionName,
                isRelease = false,
                javaSourceDirs = listOf(File(projectDir, "src/main/java")),
                resDirs = listOf(File(projectDir, "src/main/res")),
                manifestFile = File(projectDir, "AndroidManifest.xml")
            )

            val listener = object : BuildProgressListener {
                override fun onStepStarted(step: BuildStep, message: String) {}
                override fun onStepCompleted(step: BuildStep, message: String) {}
                override fun onStepFailed(step: BuildStep, error: String, details: String?) {}
                override fun onOutput(line: String) {}
                override fun onBuildCompleted(result: LightweightBuildResult) {}
            }

            val engine = LightweightBuildEngine(buildConfig, listener)
            val result = engine.build()

            // In real app, we should map LightweightBuildResult to TaskExecutionResult properly
            TaskExecutionResult(result.success, if (result.success) null else TaskExecutionResult.Failure.BUILD_FAILED)
        }
    }

    override fun cancelCurrentBuild(): CompletableFuture<BuildCancellationRequestResult> {
        return CompletableFuture.completedFuture(
            BuildCancellationRequestResult(true, null)
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
