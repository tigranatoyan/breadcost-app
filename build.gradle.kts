import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.github.spotbugs") version "6.0.9"
    checkstyle
    jacoco
    id("org.owasp.dependencycheck") version "10.0.3"
}

group = "com.breadcost"
version = "1.0.0-SNAPSHOT"
description = "Event-sourced manufacturing cost accounting system with CQRS"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven {
        name = "lombok-edge"
        url = uri("https://projectlombok.org/edge-releases")
    }
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-amqp")

    // Databases
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")            // kept for tests

    // Flyway schema migration
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Jackson for JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    // Lombok (edge for Java 21+ compatibility)
    compileOnly("org.projectlombok:lombok:edge-SNAPSHOT")
    annotationProcessor("org.projectlombok:lombok:edge-SNAPSHOT")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Validation
    implementation("jakarta.validation:jakarta.validation-api")

    // Apache POI (Excel export)
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // OpenAPI / Swagger documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks {
    bootJar {
        mainClass = "com.breadcost.BreadCostApplication"
    }

    test {
        useJUnitPlatform()
    }

    withType<JavaCompile> {
        options.compilerArgs = listOf(
            "-parameters",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.base/java.util=ALL-UNNAMED"
        )
    }
}

// Extra properties for multi-module builds
ext["java.version"] = "21"

// ── Checkstyle ────────────────────────────────────────────────────────────
checkstyle {
    toolVersion = "10.14.2"
    isIgnoreFailures = true          // warn only, don't block builds initially
    configFile = file("config/checkstyle/checkstyle.xml")
}

// ── SpotBugs ──────────────────────────────────────────────────────────────
spotbugs {
    ignoreFailures = true            // warn only
    effort = com.github.spotbugs.snom.Effort.MAX
    reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
}
tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports {
        create("html") { required = true }
        create("xml")  { required = false }
    }
}

// ── JaCoCo ────────────────────────────────────────────────────────────────
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

// ── OWASP Dependency-Check ────────────────────────────────────────────────
dependencyCheck {
    failBuildOnCVSS = 9.0f          // only fail on CRITICAL initially
    formats = listOf("HTML", "JSON")
}
