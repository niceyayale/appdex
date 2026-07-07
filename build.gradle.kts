import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    extensions.configure<DetektExtension> {
        config.setFrom("$rootDir/config/detekt.yml")
        buildUponDefaultConfig = true
    }
    // KMP 项目的 detekt 任务按 target 拆分(detektJvmMain/detektMetadataMain 等),
    // 默认 detekt 任务只扫 src/main/kotlin(KMP 下为空 → NO-SOURCE)。
    // 这里让 detekt 聚合所有 per-source-set detekt 任务。
    tasks.matching { it.name == "detekt" }.configureEach {
        dependsOn(tasks.withType<Detekt>().matching {
            it.name != "detekt" &&
                !it.name.startsWith("detektBaseline") &&
                !it.name.startsWith("detektGenerate")
        })
    }
}
