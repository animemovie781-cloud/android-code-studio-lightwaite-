package com.tom.rv2ide.build.lightweight

import com.google.gson.Gson
import java.io.File

data class LightweightProjectConfig(
    val name: String,
    val applicationId: String,
    val minSdk: Int = 21,
    val targetSdk: Int = 34,
    val versionCode: Int = 1,
    val versionName: String = "1.0",
    val mainActivity: String,
    val buildMode: String = "lightweight"
) {
    companion object {
        fun fromFile(file: File): LightweightProjectConfig? {
            if (!file.exists()) return null
            return try {
                Gson().fromJson(file.readText(), LightweightProjectConfig::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
