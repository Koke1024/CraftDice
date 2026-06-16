import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.sqldelight.coroutines)

            implementation(libs.napier)

            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "com.koke1024.craftdice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.koke1024.craftdice"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

sqldelight {
    databases {
        create("CraftDiceDatabase") {
            packageName.set("com.koke1024.craftdice.data.local")
        }
    }
}

// SQLDelight のマイグレーション検証タスク（verify*Migration）は内部で sqlite-jdbc を使用し、
// ネイティブライブラリを `java.io.tmpdir` に展開する。このタスクは Gradle の process isolation
// worker（別 JVM）で実行される。一部の Windows 環境では worker JVM の `java.io.tmpdir` が
// `C:\Windows`（書込不可）に解決され、展開に失敗してタスクのみがエラーになる。
//
// SQLDelight 2.3.2 (PR #5215) では worker の環境変数 TMP/TMPDIR に「Gradle デーモンの
// java.io.tmpdir」を引き渡すようになった。そこで verify タスク実行直前（@TaskAction より前に
// 走る doFirst・デーモン JVM 内）でデーモンの java.io.tmpdir を書込可能な build ディレクトリへ
// 向けておけば、その値が worker に伝播して展開に成功する。
// build ディレクトリは全プラットフォームで書込可能なためクロスプラットフォームに安全で、
// DB スキーマ／マイグレーションのロジックには一切手を加えない。
tasks.withType<app.cash.sqldelight.gradle.VerifyMigrationTask>().configureEach {
    val sqliteTmpDir = layout.buildDirectory.dir("sqlite-tmp").get().asFile
    doFirst {
        sqliteTmpDir.mkdirs()
        System.setProperty("java.io.tmpdir", sqliteTmpDir.absolutePath)
    }
}
