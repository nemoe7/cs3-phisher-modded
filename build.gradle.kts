import com.android.build.gradle.BaseExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        // mavenLocal()
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("com.github.nemoe7:cs3-gradle-plugin:master-SNAPSHOT")
        // classpath("com.lagradost.cloudstream3:gradle:local-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) = extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "https://github.com/nemoe7/cs3-phisher-modded")
        authors = listOf("Phisher98", "nemoe7")
    }

    android {
        namespace = "com.phisher98"

        defaultConfig {
            minSdk = 21
            compileSdkVersion(35)
            targetSdk = 35
            testInstrumentationRunner("androidx.test.runner.AndroidJUnitRunner")
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }


        tasks.withType<KotlinJvmCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_1_8)
                freeCompilerArgs.addAll(
                    "-Xno-call-assertions",
                    "-Xno-param-assertions",
                    "-Xno-receiver-assertions"
                )
            }
        }
    }

    dependencies {
        val implementation by configurations
        val cloudstream by configurations
        cloudstream("com.lagradost:cloudstream3:pre-release")

        implementation(kotlin("stdlib"))
        implementation("com.github.Blatzar:NiceHttp:0.4.11")
        implementation("org.jsoup:jsoup:1.18.3")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
        implementation("org.mozilla:rhino:1.8.0")
        implementation("me.xdrop:fuzzywuzzy:1.4.0")
        implementation("com.google.code.gson:gson:2.11.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        implementation("app.cash.quickjs:quickjs-android:0.9.2")
        implementation("com.github.vidstige:jadb:v1.2.1")

        val testImplementation by configurations
        testImplementation(kotlin("test"))
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testImplementation("io.mockk:mockk:1.13.4")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")

        val runtimeOnly by configurations
        runtimeOnly("org.slf4j:slf4j-nop:2.0.9")

        val androidTestImplementation by configurations
        androidTestImplementation("androidx.benchmark:benchmark-junit4:1.2.0")
        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        androidTestImplementation("androidx.test:runner:1.5.2")
        androidTestImplementation("androidx.test:rules:1.5.0")
        androidTestImplementation("androidx.test:core:1.5.0")
        androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")


    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        jvmArgs("-XX:+EnableDynamicAgentLoading")
    }
}

task<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
