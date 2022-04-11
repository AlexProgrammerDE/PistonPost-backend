plugins {
    java
}

group = "net.pistonmaster"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    setOf("core", "auth", "forms").forEach {
        implementation("io.dropwizard:dropwizard-$it:4.0.0-beta.1")
    }

    implementation("com.auth0:java-jwt:3.19.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
