package com.tom.rv2ide.build.lightweight

import android.content.Context
import com.blankj.utilcode.util.ResourceUtils
import com.tom.rv2ide.managers.ToolsManager
import com.tom.rv2ide.utils.Environment
import java.io.File
import java.util.concurrent.CompletableFuture
import org.slf4j.LoggerFactory

class LightweightToolManager(private val context: Context) {
    private val log = LoggerFactory.getLogger(LightweightToolManager::class.java)

    fun ensureToolsReady(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                // We extract required jars from common assets if they don't exist
                extractTool("d8.jar", Environment.D8_JAR)
                extractTool("ecj.jar", Environment.ECJ_JAR)
                extractTool("apksigner.jar", Environment.APKSIGNER_JAR)
                
                // Zipalign is usually a native binary, we'll try to extract it
                extractNativeTool("libzipalign.so", Environment.ZIPALIGN)
                
                // Generate keystore if missing
                if (!Environment.DEBUG_KEYSTORE.exists()) {
                    generateDebugKeystore()
                }
                
                true
            } catch (e: Exception) {
                log.error("Failed to setup lightweight tools", e)
                false
            }
        }
    }

    private fun extractTool(assetName: String, destFile: File) {
        if (!destFile.exists()) {
            val success = ResourceUtils.copyFileFromAssets(
                ToolsManager.getCommonAsset(assetName),
                destFile.absolutePath
            )
            if (!success) {
                log.warn("Could not extract $assetName from assets.")
            }
        }
    }

    private fun extractNativeTool(libName: String, destFile: File) {
        if (!destFile.exists()) {
            val nativeLibraryDir = context.applicationInfo.nativeLibraryDir
            val sourceLib = File(nativeLibraryDir, libName)
            if (sourceLib.exists()) {
                sourceLib.copyTo(destFile, overwrite = true)
                destFile.setExecutable(true)
            } else {
                log.warn("Native library $libName not found in $nativeLibraryDir")
            }
        }
    }

    private fun generateDebugKeystore() {
        try {
            val keytool = File(Environment.JAVA_HOME, "bin/keytool").absolutePath
            val process = ProcessBuilder(
                keytool,
                "-genkeypair",
                "-keystore", Environment.DEBUG_KEYSTORE.absolutePath,
                "-storepass", "android",
                "-alias", "androiddebugkey",
                "-keypass", "android",
                "-dname", "CN=Android Debug,O=Android,C=US",
                "-validity", "10000",
                "-keyalg", "RSA",
                "-keysize", "2048"
            ).start()
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                log.error("Failed to generate debug keystore")
            }
        } catch (e: Exception) {
            log.error("Exception generating debug keystore", e)
        }
    }
}
