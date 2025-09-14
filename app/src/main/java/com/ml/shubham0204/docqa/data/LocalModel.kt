package com.ml.shubham0204.docqa.data

import android.content.Context
import androidx.core.net.toUri
import java.nio.file.Paths
import kotlin.io.path.exists

data class LocalModel(
    val name: String,
    val description: String,
    val downloadUrl: String,
    var isLoaded: Boolean = false,
) {
    fun getFileName(): String = downloadUrl.toUri().lastPathSegment ?: throw Exception("Invalid URL")

    fun getLocalModelPath(filesDirPath: String): String = Paths.get(filesDirPath, getFileName()).toString()

    fun isDownloaded(filesDirPath: String): Boolean = Paths.get(getLocalModelPath(filesDirPath)).exists()
}
