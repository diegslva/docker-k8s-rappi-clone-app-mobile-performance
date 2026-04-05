package com.rappiclone.media.service

import com.rappiclone.domain.errors.DomainException
import com.rappiclone.media.model.*
import com.rappiclone.media.repository.MediaRepository
import com.rappiclone.media.storage.ObjectStorage
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.UUID

class MediaServiceTest {

    private val mediaRepository = mockk<MediaRepository>()
    private val objectStorage = mockk<ObjectStorage>()
    private val imageProcessor = ImageProcessor()
    private val mediaService = MediaService(mediaRepository, objectStorage, imageProcessor, "test-bucket")

    private val tenantId = "londrina-pr"
    private val ownerId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        clearAllMocks()
        coEvery { objectStorage.ensureBucket(any()) } just Runs
        coEvery { objectStorage.upload(any(), any(), any(), any(), any()) } just Runs
        coEvery { objectStorage.getUrl(any(), any()) } answers {
            "http://minio:9000/${firstArg<String>()}/${secondArg<String>()}"
        }
    }

    @Test
    fun `deve rejeitar tipo de arquivo nao permitido`() = runTest {
        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest {
                mediaService.upload(tenantId, ownerId, OwnerType.USER, MediaCategory.AVATAR,
                    "virus.exe", "application/x-executable", ByteArray(100))
            }
        }
    }

    @Test
    fun `deve rejeitar arquivo muito grande pra avatar`() = runTest {
        val bigFile = ByteArray(6 * 1024 * 1024) // 6MB > 5MB limit
        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest {
                mediaService.upload(tenantId, ownerId, OwnerType.USER, MediaCategory.AVATAR,
                    "big.jpg", "image/jpeg", bigFile)
            }
        }
    }

    @Test
    fun `deve rejeitar documento muito grande`() = runTest {
        val bigDoc = ByteArray(26 * 1024 * 1024) // 26MB > 25MB limit
        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest {
                mediaService.upload(tenantId, ownerId, OwnerType.COURIER, MediaCategory.DOCUMENT,
                    "big.pdf", "application/pdf", bigDoc)
            }
        }
    }

    @Test
    fun `deve aceitar imagem jpeg dentro do limite`() = runTest {
        val smallJpeg = createMinimalJpeg()

        coEvery { mediaRepository.create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            MediaResponse("id-1", tenantId, ownerId.toString(), OwnerType.USER, MediaCategory.AVATAR,
                "photo.jpg", "image/jpeg", smallJpeg.size.toLong(), "http://url", "http://thumb", 1, 1)

        val result = mediaService.upload(tenantId, ownerId, OwnerType.USER, MediaCategory.AVATAR,
            "photo.jpg", "image/jpeg", smallJpeg)

        assertEquals("photo.jpg", result.originalName)
        coVerify { objectStorage.ensureBucket("test-bucket") }
        coVerify { objectStorage.upload("test-bucket", any(), any(), "image/jpeg", any()) }
    }

    @Test
    fun `deve aceitar PDF como documento`() = runTest {
        val pdfBytes = ByteArray(1024) // simula PDF pequeno

        coEvery { mediaRepository.create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            MediaResponse("id-2", tenantId, ownerId.toString(), OwnerType.COURIER, MediaCategory.DOCUMENT,
                "cnh.pdf", "application/pdf", pdfBytes.size.toLong(), "http://url", null, null, null)

        val result = mediaService.upload(tenantId, ownerId, OwnerType.COURIER, MediaCategory.DOCUMENT,
            "cnh.pdf", "application/pdf", pdfBytes)

        assertEquals("cnh.pdf", result.originalName)
        assertNull(result.thumbnailUrl) // docs nao tem thumbnail
    }

    @Test
    fun `getById deve retornar media existente`() = runTest {
        val mediaId = UUID.randomUUID().toString()
        val media = MediaResponse(mediaId, tenantId, ownerId.toString(), OwnerType.STORE, MediaCategory.STORE_LOGO,
            "logo.png", "image/png", 5000, "http://url", "http://thumb", 300, 300)

        coEvery { mediaRepository.findById(UUID.fromString(mediaId), tenantId) } returns media

        val result = mediaService.getById(mediaId, tenantId)
        assertEquals(mediaId, result.id)
        assertEquals("logo.png", result.originalName)
    }

    @Test
    fun `getById deve lancar excecao pra media inexistente`() = runTest {
        coEvery { mediaRepository.findById(any(), any()) } returns null

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { mediaService.getById(UUID.randomUUID().toString(), tenantId) }
        }
    }

    @Test
    fun `delete deve remover do storage e do banco`() = runTest {
        val mediaId = UUID.randomUUID()
        val deleteInfo = MediaRepository.MediaDeleteInfo("test-bucket", "key.jpg", "key_thumb.jpg")

        coEvery { mediaRepository.delete(mediaId, tenantId) } returns deleteInfo
        coEvery { objectStorage.delete(any(), any()) } just Runs

        mediaService.delete(mediaId.toString(), tenantId)

        coVerify { objectStorage.delete("test-bucket", "key.jpg") }
        coVerify { objectStorage.delete("test-bucket", "key_thumb.jpg") }
    }

    @Test
    fun `delete deve lancar excecao pra media inexistente`() = runTest {
        coEvery { mediaRepository.delete(any(), any()) } returns null

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { mediaService.delete(UUID.randomUUID().toString(), tenantId) }
        }
    }

    @Test
    fun `AllowedContentTypes deve aceitar tipos corretos`() {
        assertTrue(AllowedContentTypes.isImage("image/jpeg"))
        assertTrue(AllowedContentTypes.isImage("image/png"))
        assertTrue(AllowedContentTypes.isImage("image/webp"))
        assertFalse(AllowedContentTypes.isImage("application/pdf"))

        assertTrue(AllowedContentTypes.isDocument("application/pdf"))
        assertTrue(AllowedContentTypes.isDocument("image/jpeg"))
        assertFalse(AllowedContentTypes.isDocument("image/webp"))

        assertFalse(AllowedContentTypes.isAllowed("application/x-executable"))
        assertFalse(AllowedContentTypes.isAllowed("text/html"))
    }

    @Test
    fun `SizeLimits deve retornar limites corretos por categoria`() {
        assertEquals(5L * 1024 * 1024, SizeLimits.maxBytesFor(MediaCategory.AVATAR))
        assertEquals(25L * 1024 * 1024, SizeLimits.maxBytesFor(MediaCategory.DOCUMENT))
        assertEquals(10L * 1024 * 1024, SizeLimits.maxBytesFor(MediaCategory.PRODUCT_PHOTO))
        assertEquals(10L * 1024 * 1024, SizeLimits.maxBytesFor(MediaCategory.STORE_LOGO))
    }

    @Test
    fun `ThumbnailSizes deve retornar dimensoes corretas`() {
        assertEquals(200, ThumbnailSizes.forCategory(MediaCategory.AVATAR)?.width)
        assertEquals(300, ThumbnailSizes.forCategory(MediaCategory.STORE_LOGO)?.width)
        assertEquals(800, ThumbnailSizes.forCategory(MediaCategory.STORE_BANNER)?.width)
        assertEquals(400, ThumbnailSizes.forCategory(MediaCategory.PRODUCT_PHOTO)?.width)
        assertNull(ThumbnailSizes.forCategory(MediaCategory.DOCUMENT))
    }

    /**
     * Cria um JPEG minimo valido (1x1 pixel) pra testes.
     */
    private fun createMinimalJpeg(): ByteArray {
        val img = java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val baos = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(img, "jpeg", baos)
        return baos.toByteArray()
    }
}
