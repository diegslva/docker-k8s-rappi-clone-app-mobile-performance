package com.rappiclone.media.service

import com.rappiclone.media.model.ThumbnailSizes
import net.coobird.thumbnailator.Thumbnails
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Processamento de imagens: resize, thumbnail, leitura de dimensoes.
 * Usa Thumbnailator (leve, sem dependencia nativa).
 */
class ImageProcessor {

    data class ImageDimensions(val width: Int, val height: Int)

    fun readDimensions(imageBytes: ByteArray): ImageDimensions? {
        return try {
            val image = ImageIO.read(ByteArrayInputStream(imageBytes)) ?: return null
            ImageDimensions(image.width, image.height)
        } catch (e: Exception) {
            null
        }
    }

    fun generateThumbnail(
        imageBytes: ByteArray,
        targetWidth: Int,
        targetHeight: Int,
        outputFormat: String = "jpeg"
    ): ByteArray {
        val output = ByteArrayOutputStream()
        Thumbnails.of(ByteArrayInputStream(imageBytes))
            .size(targetWidth, targetHeight)
            .keepAspectRatio(true)
            .outputFormat(outputFormat)
            .outputQuality(0.85)
            .toOutputStream(output)
        return output.toByteArray()
    }
}
