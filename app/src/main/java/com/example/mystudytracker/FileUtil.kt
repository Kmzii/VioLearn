package com.example.mystudytracker

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtil {
    fun from(context: Context, uri: Uri): File {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val file = createTemporalFileFrom(inputStream, context)
        inputStream?.close()
        return file
    }

    private fun createTemporalFileFrom(inputStream: InputStream?, context: Context): File {
        var targetFile: File? = null

        if (inputStream != null) {
            var read: Int
            val buffer = ByteArray(8 * 1024)

            targetFile = File(context.cacheDir, "temp_file_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(targetFile)

            while (true) {
                read = inputStream.read(buffer)
                if (read == -1) {
                    break
                }
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
            try {
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return targetFile!!
    }

    fun deleteTempFiles(context: Context) {
        val cacheDir = context.cacheDir
        val tempFilePrefix = "temp_file_"
        val tempFileSuffix = ".jpg"

        val tempFiles = cacheDir.listFiles { file ->
            file.name.startsWith(tempFilePrefix) && file.name.endsWith(tempFileSuffix)
        }

        tempFiles?.forEach { tempFile ->
            Log.d("FileDeletion", "Deleting temp file: ${tempFile.absolutePath}")
            val deletionResult = tempFile.delete()
            Log.d("FileDeletion", "Deletion result: $deletionResult")
        }
    }
}
