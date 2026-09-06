# ============================================================
# DiaCalendar2 R8 / ProGuard 규칙
# ============================================================
# Play Console "앱 최적화 - 난독화" 경고 해결을 위해 R8을 활성화하면서 추가.
# 리플렉션에 의존하는 라이브러리들이 깨지지 않도록 keep 규칙을 명시한다.

# 크래시 스택 트레이스에서 원본 줄 번호를 보기 위해 유지.
# (Play Console에 mapping.txt가 자동 업로드되어 역난독화됨)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 리플렉션 기반 라이브러리가 제네릭/애노테이션 정보를 읽으므로 유지
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ------------------------------------------------------------
# Gson (Retrofit GsonConverterFactory)
# ------------------------------------------------------------
# 모든 DTO는 @SerializedName을 쓰므로 필드명 난독화 자체는 안전하지만,
# Gson이 리플렉션으로 인스턴스를 만들려면 클래스와 생성자가 남아 있어야 한다.
-keep class com.sonbum.diacalendar2.data.remote.dto.** { *; }

# @SerializedName이 붙은 필드는 이름이 바뀌면 안 됨
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Gson TypeToken의 제네릭 정보 유지
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ------------------------------------------------------------
# kotlinx.serialization (Navigation3 Route, 백업 .diacal 파일)
# ------------------------------------------------------------
# @Serializable 클래스의 동반 생성 serializer()를 유지
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Navigation3 Route: NavKey 직렬화로 백스택이 저장/복원되므로 유지.
# 난독화되면 프로세스 재시작 후 백스택 복원이 실패한다.
-keep class com.sonbum.diacalendar2.core.routing.Route** { *; }

# 백업/복원 데이터 모델: .diacal 파일 포맷 호환성 유지
-keep class com.sonbum.diacalendar2.domain.model.** { *; }

# ------------------------------------------------------------
# Room
# ------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------
# Koin (DI - 생성자 리플렉션 사용)
# ------------------------------------------------------------
-keep class org.koin.** { *; }
-keepclassmembers class * {
    public <init>(...);
}

# ------------------------------------------------------------
# Glance 앱 위젯 / Wear 타일
# ------------------------------------------------------------
# 위젯 Receiver/Provider는 매니페스트와 시스템이 이름으로 참조
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.action.ActionCallback { *; }

# ------------------------------------------------------------
# Firebase Cloud Messaging
# ------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }
-dontwarn com.google.firebase.**

# ------------------------------------------------------------
# RevenueCat (결제)
# ------------------------------------------------------------
-keep class com.revenuecat.purchases.** { *; }
-dontwarn com.revenuecat.purchases.**

# ------------------------------------------------------------
# Wearable Data Layer
# ------------------------------------------------------------
-keep class * extends com.google.android.gms.wearable.WearableListenerService { *; }
-keep class com.google.android.gms.wearable.** { *; }

# ------------------------------------------------------------
# WorkManager
# ------------------------------------------------------------
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ------------------------------------------------------------
# 앱 컴포넌트 (매니페스트에서 이름으로 참조)
# ------------------------------------------------------------
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.content.ContentProvider { *; }

# ------------------------------------------------------------
# 기타
# ------------------------------------------------------------
# 음력 변환 라이브러리
-keep class com.usingsky.calendar.** { *; }
-dontwarn com.usingsky.calendar.**

# OkHttp/Retrofit 플랫폼 경고 억제
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
