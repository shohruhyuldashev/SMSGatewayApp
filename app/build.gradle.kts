plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.cyberbro.smsgateway"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cyberbro.smsgateway"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST}"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/**"
        }
    }
}

// Copy user-provided app/icon.png into res/drawable before build so it can be used as launcher and notification icon
val copyAppIcon by tasks.registering(Copy::class) {
    val src = project.projectDir.resolve("icon.png")
    val dest = project.projectDir.resolve("src/main/res/drawable/app_icon.png")
    from(src)
    into(dest.parentFile)
    doFirst {
        println("Copying app/icon.png -> res/drawable/app_icon.png")
    }
}

// Ensure resource generation runs after copying icon
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(copyAppIcon)
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.room:room-ktx:2.6.0")
    add("ksp", "androidx.room:room-compiler:2.6.0")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-server-core:2.3.4")
    implementation("io.ktor:ktor-server-netty:2.3.4")
    implementation("io.ktor:ktor-network-tls:2.3.4")
    implementation("io.ktor:ktor-network-tls-certificates:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    implementation("io.ktor:ktor-server-rate-limit:2.3.4")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
}
