import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.12"
}

android {
    namespace = "com.example.ticketapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ticketapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.useJUnitPlatform()
            // Robolectric executes app code in a sandbox classloader; include those classes in JaCoCo.
            val jacocoTaskExt = it.extensions.getByType(JacocoTaskExtension::class.java)
            jacocoTaskExt.isIncludeNoLocationClasses = true
            jacocoTaskExt.excludes = listOf("jdk.internal.*")
        }
    }
}

val debugUnitTestTasks = tasks.matching { it.name == "testDebugUnitTest" }

debugUnitTestTasks.configureEach {
    finalizedBy("jacocoTestReport")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(debugUnitTestTasks)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val excludes = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes").get().asFile) {
            exclude(excludes)
        }
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get().asFile) {
            include("jacoco/testDebugUnitTest.exec")
        }
    )
}

tasks.register("printJacocoCoverage") {
    dependsOn("jacocoTestReport")

    doLast {
        val reportFile = layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.xml").get().asFile
        if (!reportFile.exists()) {
            println("JaCoCo report not found at ${reportFile.absolutePath}")
            return@doLast
        }

        val xml = reportFile.readText()
        val match = Regex("<counter type=\"LINE\" missed=\"(\\d+)\" covered=\"(\\d+)\"/>")
            .findAll(xml)
            .lastOrNull()
        val lineMissed = match?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val lineCovered = match?.groupValues?.getOrNull(2)?.toLongOrNull() ?: 0L

        val total = lineCovered + lineMissed
        val percent = if (total == 0L) 0.0 else (lineCovered.toDouble() * 100.0 / total)
        println(String.format("JaCoCo line coverage: %.2f%% (%d/%d lines)", percent, lineCovered, total))
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.0")
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.6.1")

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.firebase.bom))
    androidTestImplementation(libs.firebase.firestore)
}