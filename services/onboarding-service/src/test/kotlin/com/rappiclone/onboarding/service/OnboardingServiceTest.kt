package com.rappiclone.onboarding.service

import com.rappiclone.domain.enums.*
import com.rappiclone.domain.errors.DomainException
import com.rappiclone.onboarding.model.*
import com.rappiclone.onboarding.repository.CourierApplicationRepository
import com.rappiclone.onboarding.repository.StoreApplicationRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class OnboardingServiceTest {

    private val storeRepo = mockk<StoreApplicationRepository>()
    private val courierRepo = mockk<CourierApplicationRepository>()
    private val service = OnboardingService(storeRepo, courierRepo)
    private val tenantId = "londrina-pr"
    private val userId = UUID.randomUUID()

    private val storeApp = StoreApplicationResponse(
        id = UUID.randomUUID().toString(),
        tenantId = tenantId,
        ownerUserId = userId.toString(),
        storeName = "Padaria Central",
        storeCategory = StoreCategory.BAKERY,
        taxId = "12345678000190",
        taxRegime = TaxRegime.SIMPLES_NACIONAL,
        legalName = "Padaria Central LTDA",
        phone = "+5543999999999",
        email = "padaria@email.com",
        status = OnboardingStatus.PENDING_DOCUMENTS,
        rejectionReason = null,
        createdAt = "2026-04-05T10:00:00Z"
    )

    private val courierApp = CourierApplicationResponse(
        id = UUID.randomUUID().toString(),
        tenantId = tenantId,
        userId = userId.toString(),
        fullName = "Carlos Silva",
        cpf = "12345678901",
        phone = "+5543888888888",
        email = "carlos@email.com",
        vehicleType = CourierVehicle.MOTORCYCLE,
        vehiclePlate = "ABC1234",
        status = OnboardingStatus.PENDING_DOCUMENTS,
        rejectionReason = null,
        createdAt = "2026-04-05T10:00:00Z"
    )

    @BeforeEach
    fun setup() = clearAllMocks()

    // --- Store Application Tests ---

    @Test
    fun `deve criar store application com CNPJ valido`() = runTest {
        coEvery { storeRepo.create(any(), userId, tenantId) } returns storeApp

        val request = CreateStoreApplicationRequest(
            storeName = "Padaria Central",
            storeCategory = StoreCategory.BAKERY,
            taxId = "12345678000190",
            legalName = "Padaria Central LTDA",
            phone = "+5543999999999",
            email = "padaria@email.com",
            street = "Rua Sergipe", number = "100",
            neighborhood = "Centro", city = "Londrina", state = "PR", zipCode = "86010-000"
        )

        val result = service.createStoreApplication(request, userId, tenantId)
        assertEquals("Padaria Central", result.storeName)
        assertEquals(OnboardingStatus.PENDING_DOCUMENTS, result.status)
    }

    @Test
    fun `deve rejeitar store application com CNPJ invalido`() = runTest {
        val request = CreateStoreApplicationRequest(
            storeName = "Padaria", storeCategory = StoreCategory.BAKERY,
            taxId = "123", legalName = "LTDA", phone = "999",
            email = "x@x.com", street = "Rua", number = "1",
            neighborhood = "B", city = "C", state = "PR", zipCode = "00000"
        )

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { service.createStoreApplication(request, userId, tenantId) }
        }
    }

    @Test
    fun `deve rejeitar storeName vazio`() = runTest {
        val request = CreateStoreApplicationRequest(
            storeName = "", storeCategory = StoreCategory.BAKERY,
            taxId = "12345678000190", legalName = "LTDA", phone = "999",
            email = "x@x.com", street = "Rua", number = "1",
            neighborhood = "B", city = "C", state = "PR", zipCode = "00000"
        )

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { service.createStoreApplication(request, userId, tenantId) }
        }
    }

    @Test
    fun `deve aprovar store application`() = runTest {
        val submitted = storeApp.copy(status = OnboardingStatus.DOCUMENTS_SUBMITTED)
        val approved = storeApp.copy(status = OnboardingStatus.APPROVED)
        coEvery { storeRepo.findById(UUID.fromString(storeApp.id), tenantId) } returnsMany listOf(submitted, approved)
        coEvery { storeRepo.updateStatus(any(), tenantId, OnboardingStatus.APPROVED, any(), any()) } returns true

        val result = service.reviewStoreApplication(storeApp.id, tenantId, userId, ReviewApplicationRequest(approved = true))
        assertEquals(OnboardingStatus.APPROVED, result.status)
    }

    @Test
    fun `deve rejeitar store application com motivo`() = runTest {
        val submitted = storeApp.copy(status = OnboardingStatus.DOCUMENTS_SUBMITTED)
        val rejected = storeApp.copy(status = OnboardingStatus.REJECTED, rejectionReason = "Documentos ilegíveis")
        coEvery { storeRepo.findById(UUID.fromString(storeApp.id), tenantId) } returnsMany listOf(submitted, rejected)
        coEvery { storeRepo.updateStatus(any(), tenantId, OnboardingStatus.REJECTED, any(), any()) } returns true

        val result = service.reviewStoreApplication(storeApp.id, tenantId, userId,
            ReviewApplicationRequest(approved = false, rejectionReason = "Documentos ilegíveis"))
        assertEquals(OnboardingStatus.REJECTED, result.status)
        assertEquals("Documentos ilegíveis", result.rejectionReason)
    }

    @Test
    fun `deve rejeitar rejeicao sem motivo`() = runTest {
        coEvery { storeRepo.findById(UUID.fromString(storeApp.id), tenantId) } returns
            storeApp.copy(status = OnboardingStatus.DOCUMENTS_SUBMITTED)

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.reviewStoreApplication(storeApp.id, tenantId, userId,
                    ReviewApplicationRequest(approved = false))
            }
        }
    }

    @Test
    fun `nao deve revisar aplicacao ja aprovada`() = runTest {
        coEvery { storeRepo.findById(UUID.fromString(storeApp.id), tenantId) } returns
            storeApp.copy(status = OnboardingStatus.APPROVED)

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.reviewStoreApplication(storeApp.id, tenantId, userId,
                    ReviewApplicationRequest(approved = true))
            }
        }
    }

    // --- Courier Application Tests ---

    @Test
    fun `deve criar courier application com CPF valido`() = runTest {
        coEvery { courierRepo.create(any(), userId, tenantId) } returns courierApp

        val request = CreateCourierApplicationRequest(
            fullName = "Carlos Silva",
            cpf = "12345678901",
            phone = "+5543888888888",
            email = "carlos@email.com",
            dateOfBirth = "1990-05-15",
            vehicleType = CourierVehicle.MOTORCYCLE,
            vehiclePlate = "ABC1234",
            cnhNumber = "12345678900",
            cnhCategory = "A",
            cnhExpiry = "2028-12-31"
        )

        val result = service.createCourierApplication(request, userId, tenantId)
        assertEquals("Carlos Silva", result.fullName)
    }

    @Test
    fun `deve rejeitar courier com CPF invalido`() = runTest {
        val request = CreateCourierApplicationRequest(
            fullName = "Carlos", cpf = "123",
            phone = "999", email = "x@x.com",
            dateOfBirth = "1990-01-01",
            vehicleType = CourierVehicle.BICYCLE
        )

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { service.createCourierApplication(request, userId, tenantId) }
        }
    }

    @Test
    fun `deve exigir CNH pra moto`() = runTest {
        val request = CreateCourierApplicationRequest(
            fullName = "Carlos", cpf = "12345678901",
            phone = "999", email = "x@x.com",
            dateOfBirth = "1990-01-01",
            vehicleType = CourierVehicle.MOTORCYCLE
            // cnhNumber e cnhExpiry ausentes
        )

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { service.createCourierApplication(request, userId, tenantId) }
        }
    }

    @Test
    fun `nao deve exigir CNH pra bicicleta`() = runTest {
        coEvery { courierRepo.create(any(), userId, tenantId) } returns
            courierApp.copy(vehicleType = CourierVehicle.BICYCLE, vehiclePlate = null)

        val request = CreateCourierApplicationRequest(
            fullName = "Ana Lima", cpf = "98765432101",
            phone = "999", email = "ana@x.com",
            dateOfBirth = "1995-03-20",
            vehicleType = CourierVehicle.BICYCLE
        )

        val result = service.createCourierApplication(request, userId, tenantId)
        assertNotNull(result)
    }

    @Test
    fun `nao deve adicionar documento a aplicacao aprovada`() = runTest {
        coEvery { courierRepo.findById(UUID.fromString(courierApp.id), tenantId) } returns
            courierApp.copy(status = OnboardingStatus.APPROVED)

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.addCourierDocument(courierApp.id, tenantId,
                    AddDocumentRequest("CNH_FRONT", UUID.randomUUID().toString()))
            }
        }
    }
}
