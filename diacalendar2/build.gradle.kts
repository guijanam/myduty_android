plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
}

android {
	namespace = "com.sonbum.diacalendar2"
	compileSdk {
		version = release(36)
	}

	defaultConfig {
		applicationId = "com.sonbum.diacalendar2"
		minSdk = 28
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

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
	useLibrary("wear-sdk")
	buildFeatures {
		compose = true
	}
}

kotlin {
	compilerOptions {
		jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
	}
}

dependencies {
	implementation(libs.play.services.wearable)

	// Wear OS Tiles
	implementation("androidx.wear.tiles:tiles:1.5.0")
	implementation("androidx.wear.protolayout:protolayout:1.3.0")
	implementation("androidx.wear.protolayout:protolayout-material:1.3.0")
	implementation("androidx.wear.protolayout:protolayout-expression:1.3.0")

	// Guava (ListenableFuture)
	implementation("com.google.guava:guava:33.5.0-android")

	// Coroutines (play-services await)
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.ui.graphics)
	implementation(libs.androidx.compose.ui.tooling.preview)
	implementation(libs.androidx.compose.material)
	implementation(libs.androidx.compose.foundation)
	implementation(libs.androidx.wear.tooling.preview)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.core.splashscreen)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.compose.ui.test.junit4)
	debugImplementation(libs.androidx.compose.ui.tooling)
	debugImplementation(libs.androidx.compose.ui.test.manifest)
}