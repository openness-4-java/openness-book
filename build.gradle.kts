plugins {
    java
    kotlin("jvm") version "1.4.10"
}

group = "it.unimore.dipi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://dl.bintray.com/unimore-dipi/openness-connector")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("ch.qos.logback:logback-classic:1.0.13")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.8")
    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.10.4")
    implementation("it.unimore.dipi:openness-connector:0.0.7")
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}