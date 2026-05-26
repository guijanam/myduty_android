package com.sonbum.diacalendar2.di

import androidx.room.Room
import okhttp3.logging.HttpLoggingInterceptor
import com.sonbum.diacalendar2.data.local.OfficeWebsiteRegistry
import com.sonbum.diacalendar2.data.local.database.AppDatabase
import com.sonbum.diacalendar2.data.local.datastore.CalendarPreferences
import com.sonbum.diacalendar2.data.local.datastore.CoworkerPreferences
import com.sonbum.diacalendar2.data.local.datastore.MenuPreferences
import com.sonbum.diacalendar2.data.local.datastore.OnboardingPreferences
import com.sonbum.diacalendar2.data.local.datastore.TextSizePreferences
import com.sonbum.diacalendar2.data.local.datastore.ThemePreferences
import com.sonbum.diacalendar2.data.remote.SupabaseConfig
import com.sonbum.diacalendar2.data.remote.api.SupabaseApi
import com.sonbum.diacalendar2.data.repository.DeviceCalendarRepositoryImpl
import com.sonbum.diacalendar2.data.repository.DiaRepositoryImpl
import com.sonbum.diacalendar2.data.repository.HolidayRepositoryImpl
import com.sonbum.diacalendar2.data.repository.LocalDiaRepositoryImpl
import com.sonbum.diacalendar2.data.repository.LocalOfficeRepositoryImpl
import com.sonbum.diacalendar2.data.repository.MemoRepositoryImpl
import com.sonbum.diacalendar2.domain.usecase.BackupRestoreUseCase
import com.sonbum.diacalendar2.data.repository.OfficeRepositoryImpl
import com.sonbum.diacalendar2.data.repository.ShiftRepositoryImpl
import com.sonbum.diacalendar2.data.repository.ShiftSwapRecordRepositoryImpl
import com.sonbum.diacalendar2.data.repository.VacationRecordRepositoryImpl
import com.sonbum.diacalendar2.data.repository.VacationTypeRepositoryImpl
import com.sonbum.diacalendar2.data.repository.LateWorkRecordRepositoryImpl
import com.sonbum.diacalendar2.data.repository.LateWorkTypeRepositoryImpl
import com.sonbum.diacalendar2.data.repository.LateHolidayRecordRepositoryImpl
import com.sonbum.diacalendar2.data.repository.LateHolidayTypeRepositoryImpl
import com.sonbum.diacalendar2.data.repository.ShiftInputTypeRepositoryImpl
import com.sonbum.diacalendar2.data.repository.ShiftInputRecordRepositoryImpl
import com.sonbum.diacalendar2.data.repository.ChatNoteRepositoryImpl
import com.sonbum.diacalendar2.data.repository.CustomShiftRepositoryImpl
import com.sonbum.diacalendar2.data.repository.BackupRepositoryImpl
import com.sonbum.diacalendar2.data.repository.AuthRepositoryImpl
import com.sonbum.diacalendar2.data.repository.BoardRepositoryImpl
import com.sonbum.diacalendar2.data.repository.CoworkerRepositoryImpl
import com.sonbum.diacalendar2.data.repository.SubscriptionRepositoryImpl
import com.sonbum.diacalendar2.domain.repository.DeviceCalendarRepository
import com.sonbum.diacalendar2.domain.repository.BackupRepository
import com.sonbum.diacalendar2.domain.repository.DiaRepository
import com.sonbum.diacalendar2.domain.repository.HolidayRepository
import com.sonbum.diacalendar2.domain.repository.LocalDiaRepository
import com.sonbum.diacalendar2.domain.repository.LocalOfficeRepository
import com.sonbum.diacalendar2.domain.repository.MemoRepository
import com.sonbum.diacalendar2.domain.repository.OfficeRepository
import com.sonbum.diacalendar2.domain.repository.ShiftRepository
import com.sonbum.diacalendar2.domain.repository.ShiftSwapRecordRepository
import com.sonbum.diacalendar2.domain.repository.VacationRecordRepository
import com.sonbum.diacalendar2.domain.repository.VacationTypeRepository
import com.sonbum.diacalendar2.domain.repository.LateWorkRecordRepository
import com.sonbum.diacalendar2.domain.repository.LateWorkTypeRepository
import com.sonbum.diacalendar2.domain.repository.LateHolidayRecordRepository
import com.sonbum.diacalendar2.domain.repository.LateHolidayTypeRepository
import com.sonbum.diacalendar2.domain.repository.ShiftInputTypeRepository
import com.sonbum.diacalendar2.domain.repository.ShiftInputRecordRepository
import com.sonbum.diacalendar2.domain.repository.ChatNoteRepository
import com.sonbum.diacalendar2.domain.repository.CustomShiftRepository
import com.sonbum.diacalendar2.domain.repository.AuthRepository
import com.sonbum.diacalendar2.domain.repository.BoardRepository
import com.sonbum.diacalendar2.domain.repository.CoworkerRepository
import com.sonbum.diacalendar2.domain.repository.SubscriptionRepository
import com.sonbum.diacalendar2.presentation.coworker.CoworkerViewModel
import com.sonbum.diacalendar2.presentation.subscription.PaywallViewModel
import com.sonbum.diacalendar2.presentation.coworker.CoworkerGroupViewModel
import com.sonbum.diacalendar2.presentation.coworker.CoworkerEditViewModel
import com.sonbum.diacalendar2.presentation.calendar.CalendarSelectionViewModel
import com.sonbum.diacalendar2.presentation.home.DateDetailViewModel
import com.sonbum.diacalendar2.presentation.home.HomeViewModel
import com.sonbum.diacalendar2.presentation.memo.MemoEditViewModel
import com.sonbum.diacalendar2.presentation.profile.ProfileViewModel
import com.sonbum.diacalendar2.presentation.diatable.DiaTableViewModel
import com.sonbum.diacalendar2.presentation.diatable.ServerDiaEditViewModel
import com.sonbum.diacalendar2.presentation.diatable.ServerOfficeEditViewModel
import com.sonbum.diacalendar2.presentation.localdia.LocalDiaEditViewModel
import com.sonbum.diacalendar2.presentation.localdia.LocalDiaListViewModel
import com.sonbum.diacalendar2.presentation.localoffice.LocalOfficeEditViewModel
import com.sonbum.diacalendar2.presentation.localoffice.LocalOfficeListViewModel
import com.sonbum.diacalendar2.presentation.customshift.CustomShiftListViewModel
import com.sonbum.diacalendar2.presentation.customshift.CustomShiftEditViewModel
import com.sonbum.diacalendar2.presentation.shift.ShiftSelectionViewModel
import com.sonbum.diacalendar2.presentation.textsize.TextSizeSettingsViewModel
import com.sonbum.diacalendar2.presentation.vacation.VacationSettingViewModel
import com.sonbum.diacalendar2.presentation.auth.AuthViewModel
import com.sonbum.diacalendar2.presentation.main.MainViewModel

import com.sonbum.diacalendar2.presentation.board.BoardListViewModel
import com.sonbum.diacalendar2.presentation.board.MyPostsViewModel
import com.sonbum.diacalendar2.presentation.board.PostDetailViewModel
import com.sonbum.diacalendar2.presentation.board.PostEditViewModel
import com.sonbum.diacalendar2.presentation.board.PostWriteViewModel
import com.sonbum.diacalendar2.presentation.board.BlockedUsersViewModel
import com.sonbum.diacalendar2.data.local.datastore.AuthPreferences
import com.sonbum.diacalendar2.data.local.datastore.VipPreferences
import com.sonbum.diacalendar2.data.remote.BoardSupabaseConfig
import com.sonbum.diacalendar2.data.remote.MenuSupabaseConfig
import com.sonbum.diacalendar2.data.remote.api.SupabaseBoardApi
import com.sonbum.diacalendar2.data.remote.api.SupabaseMenuApi
import com.sonbum.diacalendar2.data.repository.MenuRepositoryImpl
import com.sonbum.diacalendar2.domain.repository.MenuRepository
import com.sonbum.diacalendar2.presentation.menu.MenuViewModel
import org.koin.core.qualifier.named
import com.sonbum.diacalendar2.core.notification.AlarmScheduler
import com.sonbum.diacalendar2.core.notification.NotificationHelper
import com.sonbum.diacalendar2.data.local.datastore.CrewPatternPreferences
import com.sonbum.diacalendar2.data.local.datastore.NotificationPreferences
import androidx.work.WorkManager
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Database 모듈
 * Room Database와 DAO를 등록합니다.
 */
val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "diacalendar.db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20,
                AppDatabase.MIGRATION_20_21,
                AppDatabase.MIGRATION_21_22
            )
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().memoDao() }
    single { get<AppDatabase>().holidayDao() }
    single { get<AppDatabase>().officeDao() }
    single { get<AppDatabase>().diaDao() }
    single { get<AppDatabase>().userShiftConfigDao() }
    single { get<AppDatabase>().shiftScheduleDao() }
    single { get<AppDatabase>().vacationTypeDao() }
    single { get<AppDatabase>().vacationRecordDao() }
    single { get<AppDatabase>().localOfficeDao() }
    single { get<AppDatabase>().localDiaDao() }
    single { get<AppDatabase>().shiftSwapRecordDao() }
    single { get<AppDatabase>().lateWorkRecordDao() }
    single { get<AppDatabase>().lateWorkTypeDao() }
    single { get<AppDatabase>().lateHolidayRecordDao() }
    single { get<AppDatabase>().lateHolidayTypeDao() }
    single { get<AppDatabase>().shiftInputTypeDao() }
    single { get<AppDatabase>().shiftInputRecordDao() }
    single { get<AppDatabase>().chatNoteDao() }
    single { get<AppDatabase>().customShiftDao() }
    single { get<AppDatabase>().officeEditBackupDao() }
    single { get<AppDatabase>().diaEditBackupDao() }
    single { get<AppDatabase>().coworkerDao() }
    single { get<AppDatabase>().coworkerGroupDao() }
}

/**
 * DataStore 모듈
 * DataStore 관련 의존성을 등록합니다.
 */
val dataStoreModule = module {
    single { CalendarPreferences(androidContext()) }
    single { ThemePreferences(androidContext()) }
    single { OnboardingPreferences(androidContext()) }
    single { TextSizePreferences(androidContext()) }
    single { NotificationPreferences(androidContext()) }
    single { AuthPreferences(androidContext()) }
    single { CrewPatternPreferences(androidContext()) }
    single { MenuPreferences(androidContext()) }
    single { CoworkerPreferences(androidContext()) }
    single { OfficeWebsiteRegistry(androidContext()) }
    single { VipPreferences(androidContext()) }
}

/**
 * Repository 모듈
 * Repository 클래스들을 등록합니다.
 */
val repositoryModule = module {
    singleOf(::MemoRepositoryImpl) bind MemoRepository::class
    single<DeviceCalendarRepository> { DeviceCalendarRepositoryImpl(androidContext(), get()) }
    single<HolidayRepository> { HolidayRepositoryImpl(get(), get()) }
    single<OfficeRepository> { OfficeRepositoryImpl(get(), get(), get()) }
    single<DiaRepository> { DiaRepositoryImpl(get(), get(), get()) }
    single<ShiftRepository> { ShiftRepositoryImpl(get(), get()) }
    single<VacationTypeRepository> { VacationTypeRepositoryImpl(get()) }
    single<VacationRecordRepository> { VacationRecordRepositoryImpl(get()) }
    single<LocalOfficeRepository> { LocalOfficeRepositoryImpl(get()) }
    single<LocalDiaRepository> { LocalDiaRepositoryImpl(get()) }
    single<ShiftSwapRecordRepository> { ShiftSwapRecordRepositoryImpl(get()) }
    single<LateWorkRecordRepository> { LateWorkRecordRepositoryImpl(get()) }
    single<LateWorkTypeRepository> { LateWorkTypeRepositoryImpl(get()) }
    single<LateHolidayRecordRepository> { LateHolidayRecordRepositoryImpl(get()) }
    single<LateHolidayTypeRepository> { LateHolidayTypeRepositoryImpl(get()) }
    single<ShiftInputTypeRepository> { ShiftInputTypeRepositoryImpl(get()) }
    single<ShiftInputRecordRepository> { ShiftInputRecordRepositoryImpl(get()) }
    singleOf(::ChatNoteRepositoryImpl) bind ChatNoteRepository::class
    single<CustomShiftRepository> { CustomShiftRepositoryImpl(get()) }
    single<BackupRepository> {
        BackupRepositoryImpl(
            context = androidContext(),
            memoDao = get(),
            userShiftConfigDao = get(),
            shiftScheduleDao = get(),
            vacationTypeDao = get(),
            vacationRecordDao = get(),
            shiftSwapRecordDao = get(),
            shiftInputTypeDao = get(),
            shiftInputRecordDao = get(),
            lateWorkTypeDao = get(),
            lateWorkRecordDao = get(),
            lateHolidayTypeDao = get(),
            lateHolidayRecordDao = get(),
            localOfficeDao = get(),
            localDiaDao = get(),
            chatNoteDao = get()
        )
    }
    singleOf(::BackupRestoreUseCase)
    single<AuthRepository> { AuthRepositoryImpl(get(named("boardApi")), get()) }
    single<BoardRepository> { BoardRepositoryImpl(get(named("boardApi")), get()) }
    single<MenuRepository> { MenuRepositoryImpl(get(named("menuApi"))) }
    single<CoworkerRepository> { CoworkerRepositoryImpl(get(), get(), get()) }
    single<SubscriptionRepository> { SubscriptionRepositoryImpl(get(), get()) }
}

/**
 * ViewModel 모듈
 * 모든 ViewModel을 등록합니다.
 */
val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModel { DateDetailViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), androidContext()) }
    viewModel { MemoEditViewModel(get(), get(), androidContext()) }
    viewModelOf(::CalendarSelectionViewModel)
    viewModel { ProfileViewModel(get(), get(), get(), androidContext()) }
    viewModel { ShiftSelectionViewModel(get(), get(), get(), get(), get(), androidContext()) }
    viewModelOf(::DiaTableViewModel)
    viewModelOf(::VacationSettingViewModel)
    viewModelOf(::LocalOfficeListViewModel)
    viewModelOf(::LocalOfficeEditViewModel)
    viewModelOf(::LocalDiaListViewModel)
    viewModelOf(::LocalDiaEditViewModel)
    viewModelOf(::TextSizeSettingsViewModel)
    viewModelOf(::CustomShiftListViewModel)
    viewModelOf(::CustomShiftEditViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::BoardListViewModel)
    viewModelOf(::MyPostsViewModel)
    viewModelOf(::PostDetailViewModel)
    viewModelOf(::PostEditViewModel)
    viewModelOf(::PostWriteViewModel)
    viewModelOf(::BlockedUsersViewModel)
    viewModelOf(::ServerDiaEditViewModel)
    viewModelOf(::ServerOfficeEditViewModel)
    viewModelOf(::MenuViewModel)
    viewModelOf(::CoworkerViewModel)
    viewModelOf(::CoworkerGroupViewModel)
    viewModelOf(::CoworkerEditViewModel)
    viewModelOf(::PaywallViewModel)
}

/**
 * Network 모듈
 * Retrofit, OkHttp 등 네트워크 관련 의존성을 등록합니다.
 */
val networkModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.url)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<SupabaseApi> { get<Retrofit>().create(SupabaseApi::class.java) }

    // 게시판용 OkHttpClient (Content-Type 헤더 + 로깅 + 토큰 자동 갱신)
    single(named("boardOkHttp")) {
        val authRepository: AuthRepository by lazy { get() }
        get<OkHttpClient>().newBuilder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Content-Type", "application/json")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .authenticator { _, response ->
                // auth 엔드포인트는 갱신 대상이 아님
                if (response.request.url.encodedPath.contains("auth/v1/")) return@authenticator null
                // 무한 루프 방지: 이미 한 번 재시도했으면 포기
                if (response.request.header("X-Retry") != null) {
                    // 재시도도 실패 → 세션 만료, 로컬 세션 정리
                    runBlocking { authRepository.signOut() }
                    return@authenticator null
                }

                val newToken = runBlocking { authRepository.refreshAccessToken() }
                if (newToken == null) {
                    // refresh 실패 → 세션 만료, 로컬 세션 정리
                    runBlocking { authRepository.signOut() }
                    return@authenticator null
                }

                response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .header("X-Retry", "true")
                    .build()
            }
            .build()
    }

    // 게시판용 Supabase Retrofit (별도 프로젝트)
    single(named("boardRetrofit")) {
        Retrofit.Builder()
            .baseUrl(BoardSupabaseConfig.url)
            .client(get(named("boardOkHttp")))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<SupabaseBoardApi>(named("boardApi")) {
        get<Retrofit>(named("boardRetrofit")).create(SupabaseBoardApi::class.java)
    }

    // 식단 메뉴용 Retrofit (별도 Supabase 프로젝트)
    single(named("menuRetrofit")) {
        Retrofit.Builder()
            .baseUrl(MenuSupabaseConfig.url)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<SupabaseMenuApi>(named("menuApi")) {
        get<Retrofit>(named("menuRetrofit")).create(SupabaseMenuApi::class.java)
    }
}

/**
 * 모든 Koin 모듈을 합친 리스트
 */
/**
 * Notification 모듈
 * 알림 관련 의존성을 등록합니다.
 */
val notificationModule = module {
    single { NotificationHelper(androidContext()) }
    single { AlarmScheduler(androidContext()) }
    single { WorkManager.getInstance(androidContext()) }
}

val appModules = listOf(
    databaseModule,
    dataStoreModule,
    networkModule,
    repositoryModule,
    notificationModule,
    viewModelModule
)
