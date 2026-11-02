plugins { kotlin("multiplatform") version "2.1.0" }

kotlin {
    js(IR) {
        nodejs()
        compilerOptions { sourceMap.set(true); sourceMapEmbedSources.set(org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS) }
        binaries.library()
        useEsModules()
        generateTypeScriptDefinitions()
    }
    sourceSets {
        commonMain {
            kotlin.srcDir("../../composeApp/src/commonMain/kotlin")
            kotlin.include("com/koke1024/craftdice/domain/**")
            kotlin.include("com/koke1024/craftdice/data/repository/MetaProgressMapper.kt")
        }
        commonTest {
            kotlin.srcDir("../../composeApp/src/commonTest/kotlin")
            kotlin.include("com/koke1024/craftdice/domain/**")
            // The original run test uses JVM reflection. The JS facade is tested
            // end-to-end in tauriApp/tests/game.test.mjs instead.
            kotlin.exclude("**/RunEngineTest.kt")
            dependencies { implementation(kotlin("test")) }
        }
        jsMain.dependencies { implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3") }
    }
}

// Use the installed Node.js; no second runtime download is needed.
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().download = false
}
