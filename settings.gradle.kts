pluginManagement {
    val springBootVersion: String by settings
    plugins {
        id("org.springframework.boot") version springBootVersion
        id("io.spring.dependency-management") version "1.1.7"
    }
}

rootProject.name = "wedding-event"

include(":backend-common")
include(":admin-console:backend")
// TODO: include(":client-console:backend") once the client API is implemented
