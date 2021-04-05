import kotlin.collections.map
import kotlin.collections.set

plugins {
    application
    kotlin("jvm") version "1.4.21"
    kotlin("plugin.serialization") version "1.4.10"
}


group = "de.stevenkuenzel"
version = "1.0"


repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-rng-simple:1.3")

    implementation("org.apache.xmlgraphics:fop:2.6")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.14")
    implementation("org.jfree:jfreechart:1.5.2")

    implementation(files("lib/LWFightingICE.jar"))
    implementation(files("lib/KotlinXML-1.0.jar"))
    implementation(files("lib/simTORCS-1.0.jar"))
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "main.Main"
    }

    // To add all of the dependencies otherwise a "NoClassDefFoundError" error
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}