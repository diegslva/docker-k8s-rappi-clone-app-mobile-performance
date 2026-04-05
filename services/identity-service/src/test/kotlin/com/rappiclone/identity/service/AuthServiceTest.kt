package com.rappiclone.identity.service

import com.rappiclone.domain.enums.UserRole
import com.rappiclone.domain.errors.DomainException
import com.rappiclone.identity.model.LoginRequest
import com.rappiclone.identity.model.RegisterRequest
import com.rappiclone.identity.repository.RefreshTokenRecord
import com.rappiclone.identity.repository.RefreshTokenRepository
import com.rappiclone.identity.repository.UserRecord
import com.rappiclone.identity.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val jwtService = JwtService(
        JwtConfig(
            secret = "test-secret-minimum-256-bits-long-for-hmac256-algorithm!!",
            issuer = "rappiclone-test",
            accessTokenExpirationMinutes = 30,
            refreshTokenExpirationDays = 30
        )
    )
    private val passwordService = PasswordService()

    private val authService = AuthService(userRepository, refreshTokenRepository, jwtService, passwordService)

    private val testUser = UserRecord(
        id = UUID.randomUUID(),
        email = "diego@rappiclone.com",
        phone = "+5543999999999",
        hashedPassword = passwordService.hash("senha123"),
        role = UserRole.CUSTOMER,
        isVerified = false,
        isActive = true,
        tenantId = "londrina-pr",
        createdAt = OffsetDateTime.now(),
        updatedAt = OffsetDateTime.now()
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `register deve criar usuario e retornar tokens`() = runTest {
        coEvery { userRepository.findByEmail(any()) } returns null
        coEvery { userRepository.findByPhone(any()) } returns null
        coEvery { userRepository.create(any(), any(), any(), any(), any()) } returns testUser
        coEvery { refreshTokenRepository.create(any(), any(), any()) } returns RefreshTokenRecord(
            UUID.randomUUID(), testUser.id, "refresh-token", OffsetDateTime.now().plusDays(30), false, OffsetDateTime.now()
        )

        val request = RegisterRequest("diego@rappiclone.com", "senha123", "+5543999999999")
        val response = authService.register(request, "londrina-pr")

        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
        assertEquals(testUser.id.toString(), response.user.id)
        assertEquals("diego@rappiclone.com", response.user.email)
        assertEquals(UserRole.CUSTOMER, response.user.role)

        coVerify { userRepository.create("diego@rappiclone.com", any(), "+5543999999999", UserRole.CUSTOMER, "londrina-pr") }
    }

    @Test
    fun `register deve rejeitar email duplicado`() = runTest {
        coEvery { userRepository.findByEmail(any()) } returns testUser

        val request = RegisterRequest("diego@rappiclone.com", "senha123")

        val exception = assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { authService.register(request, null) }
        }
        assertEquals("CONFLICT", exception.error.code)
    }

    @Test
    fun `login deve retornar tokens com credenciais corretas`() = runTest {
        coEvery { userRepository.findByEmail("diego@rappiclone.com") } returns testUser
        coEvery { refreshTokenRepository.create(any(), any(), any()) } returns RefreshTokenRecord(
            UUID.randomUUID(), testUser.id, "refresh-token", OffsetDateTime.now().plusDays(30), false, OffsetDateTime.now()
        )

        val request = LoginRequest("diego@rappiclone.com", "senha123")
        val response = authService.login(request)

        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
        assertEquals(testUser.id.toString(), response.user.id)

        // Verifica que o access token e valido
        val payload = jwtService.verifyAccessToken(response.accessToken)
        assertNotNull(payload)
        assertEquals(testUser.id.toString(), payload!!.userId)
    }

    @Test
    fun `login deve rejeitar credenciais erradas`() = runTest {
        coEvery { userRepository.findByEmail("diego@rappiclone.com") } returns testUser

        val request = LoginRequest("diego@rappiclone.com", "senha-errada")

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { authService.login(request) }
        }
    }

    @Test
    fun `login deve rejeitar usuario inexistente com timing attack prevention`() = runTest {
        coEvery { userRepository.findByEmail(any()) } returns null

        val request = LoginRequest("naoexiste@rappiclone.com", "qualquer")

        val exception = assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { authService.login(request) }
        }
        assertEquals("AUTH_INVALID_CREDENTIALS", exception.error.code)
    }

    @Test
    fun `login deve rejeitar usuario inativo`() = runTest {
        val inactiveUser = testUser.copy(isActive = false)
        coEvery { userRepository.findByEmail(any()) } returns inactiveUser

        val request = LoginRequest("diego@rappiclone.com", "senha123")

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { authService.login(request) }
        }
    }

    @Test
    fun `refresh deve emitir novos tokens e revogar antigo`() = runTest {
        val oldRefreshToken = "old-refresh-token"
        val storedToken = RefreshTokenRecord(
            UUID.randomUUID(), testUser.id, oldRefreshToken,
            OffsetDateTime.now().plusDays(30), false, OffsetDateTime.now()
        )

        coEvery { refreshTokenRepository.findByToken(oldRefreshToken) } returns storedToken
        coEvery { userRepository.findById(testUser.id) } returns testUser
        coEvery { refreshTokenRepository.revoke(oldRefreshToken) } just Runs
        coEvery { refreshTokenRepository.create(any(), any(), any()) } returns RefreshTokenRecord(
            UUID.randomUUID(), testUser.id, "new-refresh-token", OffsetDateTime.now().plusDays(30), false, OffsetDateTime.now()
        )

        val response = authService.refresh(oldRefreshToken)

        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
        coVerify { refreshTokenRepository.revoke(oldRefreshToken) }
    }

    @Test
    fun `refresh deve rejeitar token expirado`() = runTest {
        val expiredToken = RefreshTokenRecord(
            UUID.randomUUID(), testUser.id, "expired-token",
            OffsetDateTime.now().minusDays(1), false, OffsetDateTime.now()
        )

        coEvery { refreshTokenRepository.findByToken("expired-token") } returns expiredToken
        coEvery { refreshTokenRepository.revoke("expired-token") } just Runs

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { authService.refresh("expired-token") }
        }
    }

    @Test
    fun `refresh deve rejeitar token inexistente`() = runTest {
        coEvery { refreshTokenRepository.findByToken(any()) } returns null

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { authService.refresh("token-que-nao-existe") }
        }
    }

    @Test
    fun `getUser deve retornar usuario existente`() = runTest {
        coEvery { userRepository.findById(testUser.id) } returns testUser

        val user = authService.getUser(testUser.id.toString())

        assertEquals(testUser.id.toString(), user.id)
        assertEquals(testUser.email, user.email)
        assertEquals(testUser.role, user.role)
    }

    @Test
    fun `getUser deve lancar excecao pra usuario inexistente`() = runTest {
        val randomId = UUID.randomUUID()
        coEvery { userRepository.findById(randomId) } returns null

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { authService.getUser(randomId.toString()) }
        }
    }
}
