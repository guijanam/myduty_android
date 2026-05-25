package com.sonbum.diacalendar2.core.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object ImageUtils {

    private const val MEMO_IMAGES_DIR = "memo_images"

    /**
     * 갤러리에서 선택한 이미지를 앱 내부 저장소에 복사하고 파일 경로를 반환한다.
     */
    fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val imagesDir = File(context.filesDir, MEMO_IMAGES_DIR)
            if (!imagesDir.exists()) imagesDir.mkdirs()

            val fileName = "memo_${UUID.randomUUID()}.jpg"
            val destFile = File(imagesDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 내부 저장소의 메모 이미지를 삭제한다.
     */
    fun deleteImage(imagePath: String) {
        try {
            val file = File(imagePath)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
