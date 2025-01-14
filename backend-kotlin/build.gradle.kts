plugins {
    kotlin("jvm") version "1.9.22"
    application
    id("com.diffplug.spotless") version "6.25.0"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.github.aar0u.quickhub"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("org.nanohttpd:nanohttpd-apache-fileupload:2.3.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("ch.qos.logback:logback-classic:1.4.12")
    implementation("org.slf4j:slf4j-api:2.0.9")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.github.aar0u.quickhub.MainKt")
}

// Add a custom task to copy static files to resources during development
tasks.register<Copy>("copyStaticFiles") {
    from("../static")
    into("build/resources/main/static")
}

// Make the run task depend on copying static files
tasks.named("run") {
    dependsOn("copyStaticFiles")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Include static frontend resources
    from("../static") {
        into("static")
        include("**/*")
    }
}

tasks.shadowJar {
    archiveClassifier.set("fat")

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        exclude(dependency("org.jetbrains:annotations"))
        exclude(dependency("ch.qos.logback:logback-classic"))
        exclude(dependency("org.slf4j:slf4j-api"))
    }

    // Inherit configurations from the standard jar task
    from(tasks.jar.get().outputs)
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("0.50.0")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}
