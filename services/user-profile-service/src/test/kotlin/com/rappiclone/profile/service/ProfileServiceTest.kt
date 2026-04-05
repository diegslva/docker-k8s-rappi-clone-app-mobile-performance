package com.rappiclone.profile.service

import com.rappiclone.domain.errors.DomainException
import com.rappiclone.profile.model.*
import com.rappiclone.profile.repository.ProfileRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ProfileServiceTest {

    private val profileRepository = mockk<ProfileRepository>()
    private val profileService = ProfileService(profileRepository)
    private val tenantId = "londrina-pr"
    private val userId = UUID.randomUUID()

    private val testProfile = ProfileResponse(
        id = UUID.randomUUID().toString(),
        userId = userId.toString(),
        tenantId = tenantId,
        firstName = "Diego",
        lastName = "Silva",
        displayName = "Diego Silva",
        phone = "+5543999999999",
        avatarUrl = null,
        language = "pt-BR"
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `createProfile deve criar perfil com dados validos`() = runTest {
        coEvery { profileRepository.findByUserId(userId, tenantId) } returns null
        coEvery { profileRepository.create(any(), tenantId) } returns testProfile

        val request = CreateProfileRequest(
            userId = userId.toString(),
            firstName = "Diego",
            lastName = "Silva"
        )
        val result = profileService.createProfile(request, tenantId)

        assertEquals("Diego", result.firstName)
        assertEquals("Silva", result.lastName)
        assertEquals(tenantId, result.tenantId)
    }

    @Test
    fun `createProfile deve rejeitar perfil duplicado`() = runTest {
        coEvery { profileRepository.findByUserId(userId, tenantId) } returns testProfile

        val request = CreateProfileRequest(userId = userId.toString(), firstName = "Diego", lastName = "Silva")

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { profileService.createProfile(request, tenantId) }
        }
    }

    @Test
    fun `createProfile deve rejeitar firstName vazio`() = runTest {
        val request = CreateProfileRequest(userId = userId.toString(), firstName = "", lastName = "Silva")

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { profileService.createProfile(request, tenantId) }
        }
    }

    @Test
    fun `getProfile deve retornar perfil existente`() = runTest {
        coEvery { profileRepository.findByUserId(userId, tenantId) } returns testProfile

        val result = profileService.getProfile(userId.toString(), tenantId)
        assertEquals(userId.toString(), result.userId)
    }

    @Test
    fun `getProfile deve lancar excecao pra perfil inexistente`() = runTest {
        coEvery { profileRepository.findByUserId(userId, tenantId) } returns null

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { profileService.getProfile(userId.toString(), tenantId) }
        }
    }

    @Test
    fun `updateProfile deve atualizar campos fornecidos`() = runTest {
        val updated = testProfile.copy(firstName = "Diego Luiz", phone = "+5543888888888")
        coEvery { profileRepository.findByUserId(userId, tenantId) } returns testProfile
        coEvery { profileRepository.update(UUID.fromString(testProfile.id), tenantId, any()) } returns updated

        val request = UpdateProfileRequest(firstName = "Diego Luiz", phone = "+5543888888888")
        val result = profileService.updateProfile(userId.toString(), tenantId, request)

        assertEquals("Diego Luiz", result.firstName)
        assertEquals("+5543888888888", result.phone)
    }

    @Test
    fun `addAddress deve validar campos obrigatorios`() = runTest {
        coEvery { profileRepository.findByUserId(userId, tenantId) } returns testProfile

        val invalidRequest = CreateAddressRequest(
            street = "",
            number = "123",
            neighborhood = "Centro",
            city = "Londrina",
            state = "PR",
            zipCode = "86010-000"
        )

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { profileService.addAddress(userId.toString(), tenantId, invalidRequest) }
        }
    }

    @Test
    fun `addAddress deve validar UF com 2 caracteres`() = runTest {
        coEvery { profileRepository.findByUserId(userId, tenantId) } returns testProfile

        val invalidRequest = CreateAddressRequest(
            street = "Rua Sergipe",
            number = "123",
            neighborhood = "Centro",
            city = "Londrina",
            state = "PARANA",
            zipCode = "86010-000"
        )

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { profileService.addAddress(userId.toString(), tenantId, invalidRequest) }
        }
    }

    @Test
    fun `deleteAddress deve lancar excecao pra perfil inexistente`() = runTest {
        coEvery { profileRepository.findByUserId(userId, tenantId) } returns null

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest {
                profileService.deleteAddress(userId.toString(), tenantId, UUID.randomUUID().toString())
            }
        }
    }

    @Test
    fun `deleteAddress deve lancar excecao pra endereco inexistente`() = runTest {
        coEvery { profileRepository.findByUserId(userId, tenantId) } returns testProfile
        coEvery { profileRepository.deleteAddress(any(), UUID.fromString(testProfile.id)) } returns false

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest {
                profileService.deleteAddress(userId.toString(), tenantId, UUID.randomUUID().toString())
            }
        }
    }
}
