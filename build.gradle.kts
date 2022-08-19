plugins {
    application
}

application {
    mainClass.set("net.pistonmaster.pistonpost.PistonPostApplication")
}

group = "net.pistonmaster"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repository.aspose.com/repo/")
}

dependencies {
    implementation("io.dropwizard:dropwizard-core:4.0.0-beta.2")
    implementation("io.dropwizard:dropwizard-auth:4.0.0-beta.2")
    implementation("io.dropwizard:dropwizard-forms:4.0.0-beta.2")

    implementation("io.swagger.core.v3:swagger-core-jakarta:2.2.2")
    implementation("io.swagger.core.v3:swagger-jaxrs2-jakarta:2.2.2")
    implementation("io.swagger.core.v3:swagger-integration-jakarta:2.2.2")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation("com.github.javafaker:javafaker:1.0.2") {
        exclude("org.yaml")
    }

    implementation("commons-io:commons-io:2.11.0")

    implementation("com.twelvemonkeys.servlet:servlet:3.8.2:jakarta@jar")
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.8.2")

    implementation("org.sejda.imageio:webp-imageio:0.1.6")

    implementation("org.jcodec:jcodec:0.2.5")
    implementation("org.jcodec:jcodec-javase:0.2.5")
    implementation("ws.schild:jave-all-deps:3.3.1")

    // https://mvnrepository.com/artifact/org.yaml/snakeyaml
    implementation("org.yaml:snakeyaml:1.30")

    implementation("com.squareup.keywhiz:keywhiz-hkdf:0.10.1")

    // https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt
    implementation("com.nimbusds:nimbus-jose-jwt:9.24.2")

    implementation("org.mongodb:mongodb-driver-sync:4.7.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks {
    processResources {
        filter { line: String ->
            line.replace("@projectVersion@", "${rootProject.version}")
        }
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
