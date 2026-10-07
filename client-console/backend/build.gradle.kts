// WedPlanner mobile API — Spring Boot 4, port 8081, routes /api/v1/auth|users|client|messages/** and WebSocket /ws
plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":backend-common"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    // FN-MSG-01: STOMP over WebSocket (/ws, /app/chat, /topic/messages/{roomId})
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("client-backend.jar")
}

// ---- Test coverage gate (TDD) ----------------------------------------------------------------------------------
// Measures this service's own classes. backend-common is measured by the admin backend's gate (its tests exercise
// the shared admin domain); the client-only entities in backend-common are plain mappings.
// `./gradlew build` (check) fails when LINE coverage drops below the minimum; override with -PcoverageMinimum=0.90
val coverageMinimum = (findProperty("coverageMinimum") as String? ?: "0.85").toBigDecimal()
val coverageExcludes = listOf("com/weddingevent/ClientConsoleApplication*") // bootstrap main() only

fun JacocoReportBase.coverageScope() {
    classDirectories.setFrom(
        files(layout.buildDirectory.dir("classes/java/main")).asFileTree.matching { exclude(coverageExcludes.map { "$it.class" }) }
    )
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    coverageScope()
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.test)
    coverageScope()
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = coverageMinimum
            }
        }
    }
}

tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}
