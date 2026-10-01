// Shared library (not a runnable app): entities, enums, repositories, API envelope, error handling
plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework:spring-webmvc")
    api("org.springframework.security:spring-security-core")
    api("com.fasterxml.jackson.core:jackson-annotations")
    compileOnly("jakarta.servlet:jakarta.servlet-api") // provided by the web app at runtime
    implementation("org.slf4j:slf4j-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
