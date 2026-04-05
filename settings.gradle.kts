rootProject.name = "rappi-clone"

// --- Shared Modules ---
include(":shared:shared-domain")
include(":shared:shared-infra")
include(":shared:shared-test")

// --- Backend Services ---
include(":services:api-gateway")
include(":services:identity-service")
include(":services:user-profile-service")
include(":services:catalog-service")
include(":services:search-service")
include(":services:cart-service")
include(":services:order-service")
include(":services:payment-service")
include(":services:courier-service")
include(":services:tracking-service")
include(":services:notification-service")
include(":services:rating-service")
include(":services:promotion-service")
include(":services:pricing-service")
include(":services:geolocation-service")
include(":services:media-service")
include(":services:analytics-service")
include(":services:chat-service")
include(":services:support-service")
include(":services:onboarding-service")
include(":services:fiscal-service")
include(":services:settlement-service")
include(":services:recommendation-service")
include(":services:tenant-service")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}
