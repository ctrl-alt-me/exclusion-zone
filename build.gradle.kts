plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "2.0.10"
}

group = "com.exclusionzone"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.google.code.gson:gson:2.10.1")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.exclusionzone.ExclusionZonePlugin"
    }
}
