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
}

dependencies {
    implementation("io.dropwizard:dropwizard-core:4.0.7")
    implementation("io.dropwizard:dropwizard-auth:4.0.7")
    implementation("io.dropwizard:dropwizard-forms:4.0.7")

    implementation("io.swagger.core.v3:swagger-core-jakarta:2.2.22")
    implementation("io.swagger.core.v3:swagger-jaxrs2-jakarta:2.2.22")
    implementation("io.swagger.core.v3:swagger-integration-jakarta:2.2.22")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    implementation("com.github.javafaker:javafaker:1.0.2") {
        exclude("org.yaml")
    }

    implementation("commons-io:commons-io:2.16.1")

    implementation("com.twelvemonkeys.servlet:servlet:3.8.3:jakarta@jar")
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.10.1")
    implementation("com.twelvemonkeys.imageio:imageio-bmp:3.10.1")
    implementation("com.twelvemonkeys.imageio:imageio-tiff:3.10.1")

    implementation("org.apache.tika:tika-core:2.9.2")
    implementation("org.apache.tika:tika-parser-image-module:2.9.2")

    implementation("org.jcodec:jcodec:0.2.5")
    implementation("org.jcodec:jcodec-javase:0.2.5")
    implementation("ws.schild:jave-all-deps:3.5.0")

    // https://mvnrepository.com/artifact/org.yaml/snakeyaml
    implementation("org.yaml:snakeyaml:2.2")

    implementation("com.squareup.keywhiz:keywhiz-hkdf:0.10.1")

    // https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt
    implementation("com.nimbusds:nimbus-jose-jwt:9.39.3")

    implementation("org.mongodb:mongodb-driver-sync:4.11.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
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
