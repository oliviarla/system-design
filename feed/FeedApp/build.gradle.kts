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
	// GraplQL
	implementation("org.springframework.boot:spring-boot-starter-graphql")
	implementation("com.graphql-java:graphql-java")

	// DB
	implementation("org.springframework.data:spring-data-cassandra")

	// Messaging
	implementation("org.springframework.kafka:spring-kafka")

	// Testing
	testImplementation("org.springframework.kafka:spring-kafka-test")
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
