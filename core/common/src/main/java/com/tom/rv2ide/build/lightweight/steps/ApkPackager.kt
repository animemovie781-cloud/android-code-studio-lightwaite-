package com.tom.rv2ide.build.lightweight.steps

import com.tom.rv2ide.build.lightweight.BuildProgressListener
import com.tom.rv2ide.build.lightweight.BuildStep
import com.tom.rv2ide.build.lightweight.LightweightBuildConfig
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ApkPackager : BuildStepExecutor() {
    override val step = BuildStep.PACKAGE_APK
    override val stepName = "Packaging APK"

    override fun execute(config: LightweightBuildConfig, listener: BuildProgressListener): Boolean {
        listener.onStepStarted(step, stepName)
        
        val resApk = File(config.outputDir, "resources.ap_")
        val dexDir = File(config.outputDir, "dex")
        val unalignedApk = File(config.outputDir, "app-unaligned.apk")
        
        if (!resApk.exists()) {
            listener.onStepFailed(step, "Resource APK not found", null)
            return false
        }

        try {
            // Copy resources.ap_ to app-unaligned.apk and add dex files
            FileOutputStream(unalignedApk).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    
                    // First, copy all entries from resources.ap_
                    FileInputStream(resApk).use { fis ->
                        ZipInputStream(fis).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                zos.putNextEntry(ZipEntry(entry.name))
                                zis.copyTo(zos)
                                zos.closeEntry()
                                entry = zis.nextEntry
                            }
                        }
                    }
                    
                    // Second, add all .dex files from dexDir
                    if (dexDir.exists()) {
                        dexDir.listFiles { _, name -> name.endsWith(".dex") }?.forEach { dexFile ->
                            val entry = ZipEntry(dexFile.name)
                            zos.putNextEntry(entry)
                            FileInputStream(dexFile).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            
            listener.onStepCompleted(step, "APK packaged successfully")
            return true
            
        } catch (e: Exception) {
            listener.onStepFailed(step, "Failed to package APK: ${e.message}", null)
            return false
        }
    }
}
