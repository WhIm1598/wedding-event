// Lumière Studios admin API — Spring Boot 4, port 8082, routes /api/v1/admin/**
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
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    // A4 contract PDF rendering (HTML -> PDF)
    implementation("io.github.openhtmltopdf:openhtmltopdf-pdfbox:1.1.87")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("admin-backend.jar")
}

// ---- Test coverage gate (TDD) ----------------------------------------------------------------------------------
// backend-common is exercised through this service's tests, so its classes are measured here too.
// `./gradlew build` (check) fails when LINE coverage drops below the minimum; override with -PcoverageMinimum=0.90
val coverageMinimum = (findProperty("coverageMinimum") as String? ?: "0.85").toBigDecimal()
val commonProject = project(":backend-common")
val coverageExcludes = listOf("com/weddingevent/AdminConsoleApplication*") // bootstrap main() only

fun JacocoReportBase.coverageScope() {
    sourceDirectories.setFrom(files("src/main/java", commonProject.file("src/main/java")))
    classDirectories.setFrom(
        files(
            layout.buildDirectory.dir("classes/java/main"),
            commonProject.layout.buildDirectory.dir("classes/java/main"),
        ).asFileTree.matching { exclude(coverageExcludes.map { "$it.class" }) }
    )
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test, ":backend-common:classes")
    coverageScope()
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.test, ":backend-common:classes")
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
