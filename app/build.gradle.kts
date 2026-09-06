import org.jetbrains.kotlin.gradle.dsl.JvmTarget

import java.util.Properties // [추가] Properties 사용을 위한 import
import java.io.FileInputStream // [추가] 파일 읽기를 위한 import

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)

	alias(libs.plugins.ksp)

	alias(libs.plugins.jetbrains.kotlin.serialization)

	alias(libs.plugins.google.services)
}

// [추가] local.properties 파일 로드 로직
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
	localProperties.load(FileInputStream(localPropertiesFile))
}


android {
	namespace = "com.sonbum.diacalendar2"
	compileSdk {
		version = release(36)
	}

	defaultConfig {
		applicationId = "com.sonbum.diacalendar2"
		minSdk = 29
		targetSdk = 36
		versionCode = 61
		versionName = "4.8"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

		// Supabase 설정
		buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL", "")}\"")
		buildConfigField("String", "SUPABASE_KEY", "\"${localProperties.getProperty("SUPABASE_KEY", "")}\"")

		// Google OAuth
		buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")
	}

	buildTypes {
		release {
			// R8 코드 축소 + 난독화 (Play Console "앱 최적화" 요구사항)
			isMinifyEnabled = true
			// 사용하지 않는 리소스 제거 (minify와 함께여야 동작)
			isShrinkResources = true
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
		buildConfig = true
	}
}

kotlin {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_17) // "17" 대신 JvmTarget.JVM_17 사용
	}
}


dependencies {

	// 캘린더 UI
	implementation(libs.kizitonwose.calendar.compose)

	// Material
	implementation(libs.google.android.material)
	implementation(libs.androidx.compose.material.icons.extended)

	// 음력-양력 변환
	implementation(libs.korean.lunar.calendar)

	// DataStore
	implementation(libs.androidx.datastore.preferences)

	// Google Play In-App Updates
	implementation(libs.bundles.play.app.update)

	// 권한
	implementation(libs.accompanist.permissions)
	implementation(libs.accompanist.systemuicontroller)

	// Retrofit
	implementation(libs.bundles.rerofit)

	// Koin
	implementation(libs.bundles.koin)

	// 드래그앤드롭
	implementation(libs.reorderable)

	// Glance 위젯
	implementation(libs.bundles.glance)

	// 이미지 불러오기
	implementation(libs.coil.compose)

	// EncryptedSharedPreferences
	implementation(libs.androidx.security.crypto)

	// 결제시스템
	implementation(libs.bundles.revenuecat)

	// Google Credential Manager (Google Sign-In)
	implementation(libs.bundles.credentials)

	// Firebase
	implementation(platform(libs.firebase.bom))
	implementation(libs.firebase.messaging)

	// WorkManager
	implementation(libs.androidx.work.runtime.ktx)

	// Wearable Data Layer
	implementation(libs.play.services.wearable)
	implementation(libs.kotlinx.coroutines.play.services)

	// Room
	implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	implementation(libs.androidx.compose.ui.unit)
	implementation(libs.androidx.compose.ui.text)
	ksp(libs.androidx.room.compiler)

	implementation(libs.androidx.navigation3.ui)
	implementation(libs.androidx.navigation3.runtime)
	implementation(libs.androidx.lifecycle.viewmodel.navigation3)
	implementation(libs.androidx.material3.adaptive.navigation3)
	implementation(libs.kotlinx.serialization.core)
	implementation(libs.kotlinx.serialization.json)

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.ui.graphics)
	implementation(libs.androidx.compose.ui.tooling.preview)
	implementation(libs.androidx.compose.material3)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.compose.ui.test.junit4)
	debugImplementation(libs.androidx.compose.ui.tooling)
	debugImplementation(libs.androidx.compose.ui.test.manifest)
}
