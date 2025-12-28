plugins {
	kotlin("jvm") version "1.9.0"
	kotlin("plugin.spring") version "1.9.0"
	id("org.springframework.boot") version "3.4.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot
	implementation("org.springframework.boot:spring-boot-starter-web")

	// Spring Batch
	implementation("org.springframework.boot:spring-boot-starter-batch")

	// Quartz Scheduler
	implementation("org.springframework.boot:spring-boot-starter-quartz")

    // Database
    runtimeOnly("com.mysql:mysql-connector-j")

	// Redis
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.batch:spring-batch-test")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
		jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}