plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // shared-domain e puro — sem dependencias de framework
    // Apenas Kotlin stdlib + serialization + coroutines (ja vem do root)
}
