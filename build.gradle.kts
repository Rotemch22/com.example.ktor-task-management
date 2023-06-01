val ktor_version: String by project
val exposed_version: String by project
val mockkVersion: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
    id("io.ktor.plugin") version "2.3.0"
}

group = "com.example"
version = "0.0.1"
application {
    mainClass.set("com.example.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

tasks {
    // Configure the compileTestJava task
    named<JavaCompile>("compileTestJava") {
        sourceCompatibility = JavaVersion.toVersion("1.8").toString()
        targetCompatibility = JavaVersion.toVersion("1.8").toString()
    }

    // Configure the compileTestKotlin task
    named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = org.jetbrains.kotlin.config.JvmTarget.DEFAULT.description
    }
}

dependencies {
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-sessions:$ktor_version")
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")
    implementation( "org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation( "org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation( "org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation( "com.zaxxer:HikariCP:3.4.5")
    implementation( "org.postgresql:postgresql:42.2.1")
    implementation ("io.insert-koin:koin-core:3.1.2")
    implementation ("io.github.microutils:kotlin-logging:2.0.11")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.30.1")
    implementation("org.mindrot:jbcrypt:0.4")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("io.ktor:ktor-server-test-host:${ktor_version}")
}