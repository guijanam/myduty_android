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
		versionCode = 1010
		versionName = "1.2"

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
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
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

	// play-services-wearable가 transitive로 끌어오는 fragment:1.1.0 경고 해소
	implementation(libs.androidx.fragment)

	// Wear OS Tiles
	implementation(libs.bundles.wear.protolayout)

	// Guava (ListenableFuture)
	implementation(libs.guava)

	// Coroutines (play-services await)
	implementation(libs.kotlinx.coroutines.play.services)

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