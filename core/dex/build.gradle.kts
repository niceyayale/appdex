plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core:io"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit.jupiter.params)
                runtimeOnly(libs.junit.jupiter.engine)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.dexlib2)
                implementation(libs.smali)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.dexlib2)
                implementation(libs.smali)
            }
        }
    }
}
