plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ynufe"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.ynufe"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.1.2"

        testInstrumentationRunner = "com.ynufe.HiltTestRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // 核心与 UI (保留)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    // 基础图标库（通常已包含在 Material3 中，但可以显式声明）
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.core.splashscreen)
    // 扩展图标库（包含绝大多数常用图标）
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.animation:animation:1.10.6")


    // --- 网络与爬虫逻辑 ---
    implementation(libs.retrofit)
    implementation(libs.converter.scalars)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.retrofit.converter.gson)

    // --- 本地数据库 (Room) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.runner)
    implementation(libs.androidx.material3)
    ksp(libs.androidx.room.compiler)

    // --- 图片加载 (如果教务处有验证码) ---
    implementation(libs.coil.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // 测试相关 (保留)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}