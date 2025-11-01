plugins {
    kotlin("jvm") version "1.9.0"
}

group = "com.x"
version = "1.0-SNAPSHOT"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        // Kotlin
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

        // Spring
        implementation("org.springframework.boot:spring-boot-starter-webflux")
        implementation("org.springframework.boot:spring-boot-starter-validation")
        implementation("org.springframework.boot:spring-boot-starter-security")
        implementation("org.springframework.boot:spring-boot-starter-actuator")

        // JWT
        implementation("io.jsonwebtoken:jjwt-api:0.12.3")
        runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
        runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

        // Cache
        implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

        // Testing
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("io.projectreactor:reactor-test")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.test {
        useJUnitPlatform()
    }

    kotlin {
        jvmToolchain(8)
    }
}
