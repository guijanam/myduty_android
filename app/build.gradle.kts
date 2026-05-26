import org.jetbrains.kotlin.gradle.dsl.JvmTarget

import java.util.Properties // [추가] Properties 사용을 위한 import
import java.io.FileInputStream // [추가] 파일 읽기를 위한 import

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)

	alias(libs.plugins.ksp)

	alias(libs.plugins.jetbrains.kotlin.serialization)

	id("com.google.gms.google-services")
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
		versionCode = 46
		versionName = "4.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

		// Supabase 설정
		buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL", "")}\"")
		buildConfigField("String", "SUPABASE_KEY", "\"${localProperties.getProperty("SUPABASE_KEY", "")}\"")

		// Google OAuth
		buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")
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

	implementation("com.kizitonwose.calendar:compose:2.10.1")

	implementation("com.google.android.material:material:1.14.0")



	implementation("androidx.compose.material:material-icons-extended:1.7.8")

	// 음력-양력 변환
	implementation("com.github.usingsky:KoreanLunarCalendar:0.3.1")

	//datestore
	implementation("androidx.datastore:datastore-preferences:1.2.1")

	// Google Play In-App Updates
	implementation("com.google.android.play:app-update:2.1.0")
	implementation("com.google.android.play:app-update-ktx:2.1.0")

	//권한
	implementation ("com.google.accompanist:accompanist-permissions:0.37.3")
	implementation ("com.google.accompanist:accompanist-systemuicontroller:0.36.0")

	// Retrofit
	implementation(libs.bundles.rerofit)

	// Koin
	implementation(libs.bundles.koin)

	//드래그앤 드롭
	implementation("sh.calvin.reorderable:reorderable:3.1.0")

    //glance widget
	implementation("androidx.glance:glance-appwidget:1.1.1")
	implementation("androidx.glance:glance-material3:1.1.1")

	//이미지 불러오기
	implementation("io.coil-kt:coil-compose:2.7.0")

	// EncryptedSharedPreferences
	implementation("androidx.security:security-crypto:1.1.0")

	// AdMob
	implementation("com.google.android.gms:play-services-ads:25.2.0")

	//결제시스템
	implementation("com.revenuecat.purchases:purchases:10.6.1")
	implementation("com.revenuecat.purchases:purchases-ui:10.6.1")


	// Google Credential Manager (Google Sign-In)
	implementation("androidx.credentials:credentials:1.6.0")
	implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
	implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

	// Firebase
	implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
	implementation("com.google.firebase:firebase-messaging")

	// WorkManager
	implementation("androidx.work:work-runtime-ktx:2.11.2")

	// ✅ Wearable Data Layer 추가
//	implementation("com.google.android.gms:play-services-wearable:19.0.0")
//	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

	// Room (버전 카탈로그 사용)
	implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	implementation(libs.androidx.compose.ui.unit)
	implementation(libs.androidx.compose.ui.text)     // 코루틴 지원
	ksp(libs.androidx.room.compiler)           // KSP 컴파일러

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