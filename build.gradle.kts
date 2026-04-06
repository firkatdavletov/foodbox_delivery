import org.gradle.api.tasks.compile.JavaCompile

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.jpa") version "1.9.25"
	id("org.springframework.boot") version "3.4.4"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.foodbox"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	maven { url = uri("https://maven.aliyun.com/repository/public") }
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.hibernate.orm:hibernate-spatial")
	implementation("org.locationtech.jts:jts-core")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.apache.commons:commons-csv:1.11.0")
	implementation("software.amazon.awssdk:s3:2.25.0")
	implementation("software.amazon.awssdk:auth:2.25.0")
	implementation("org.sejda.imageio:webp-imageio:0.1.6")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	runtimeOnly("org.postgresql:postgresql")
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

val openApiSourceFile = layout.projectDirectory.file("openapi.yaml")
val openApiToolSourceDir = layout.projectDirectory.dir("gradle/openapi")
val openApiToolOutputDir = layout.buildDirectory.dir("openapi-tool/classes")
val generatedOpenApiDir = layout.buildDirectory.dir("openapi")
val snakeYamlClasspath = files(
	fileTree(requireNotNull(gradle.gradleHomeDir).resolve("lib/plugins")) {
		include("snakeyaml-*.jar")
	},
)

val compileOpenApiSplitter = tasks.register<JavaCompile>("compileOpenApiSplitter") {
	group = "documentation"
	description = "Compiles the OpenAPI splitter helper"

	source = fileTree(openApiToolSourceDir) {
		include("**/*.java")
	}
	classpath = snakeYamlClasspath
	destinationDirectory.set(openApiToolOutputDir)
	options.release.set(17)
}

fun registerOpenApiSplitTask(
	name: String,
	outputFileName: String,
	mode: String,
) = tasks.register(name) {
	group = "documentation"
	description = "Generates $outputFileName from openapi.yaml"

	dependsOn(compileOpenApiSplitter)
	inputs.file(openApiSourceFile)
	inputs.files(fileTree(openApiToolSourceDir) { include("**/*.java") })
	outputs.file(generatedOpenApiDir.map { it.file(outputFileName) })

	doLast {
		javaexec {
			classpath = files(openApiToolOutputDir.get().asFile, snakeYamlClasspath)
			mainClass.set("OpenApiSplitter")
			args(
				mode,
				openApiSourceFile.asFile.absolutePath,
				generatedOpenApiDir.get().file(outputFileName).asFile.absolutePath,
			)
		}
	}
}

val generatePublicOpenApi = registerOpenApiSplitTask(
	name = "generatePublicOpenApi",
	outputFileName = "public-openapi.yaml",
	mode = "public",
)

val generateAdminOpenApi = registerOpenApiSplitTask(
	name = "generateAdminOpenApi",
	outputFileName = "admin-openapi.yaml",
	mode = "admin",
)

tasks.register("generateOpenApiSpecs") {
	group = "documentation"
	description = "Generates public and admin OpenAPI specs into build/openapi"
	dependsOn(generatePublicOpenApi, generateAdminOpenApi)
}
