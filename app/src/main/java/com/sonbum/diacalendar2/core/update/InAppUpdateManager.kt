package com.sonbum.diacalendar2.core.update

import android.app.Activity
import android.content.IntentSender
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.Lifecycle
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play In-App Updates를 관리하는 클래스
 *
 * 사용법:
 * 1. MainActivity에서 InAppUpdateManager 인스턴스 생성
 * 2. registerUpdateLauncher()로 ActivityResultLauncher 등록
 * 3. checkForUpdate()로 업데이트 확인 및 시작
 * 4. onResume()에서 checkUpdateOnResume() 호출
 * 5. onDestroy()에서 unregisterListener() 호출
 */
class InAppUpdateManager(private val activity: ComponentActivity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    private var updateLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                val progress = if (totalBytesToDownload > 0) {
                    (bytesDownloaded * 100 / totalBytesToDownload).toInt()
                } else 0
                _updateState.value = UpdateState.Downloading(progress)
            }
            InstallStatus.DOWNLOADED -> {
                _updateState.value = UpdateState.Downloaded
            }
            InstallStatus.INSTALLING -> {
                _updateState.value = UpdateState.Installing
            }
            InstallStatus.INSTALLED -> {
                _updateState.value = UpdateState.Installed
            }
            InstallStatus.FAILED -> {
                _updateState.value = UpdateState.Failed("업데이트 설치 실패")
            }
            InstallStatus.CANCELED -> {
                _updateState.value = UpdateState.Canceled
            }
            else -> {}
        }
    }

    init {
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    /**
     * ActivityResultLauncher 등록
     * MainActivity의 onCreate에서 registerForActivityResult로 생성한 launcher를 전달
     */
    fun registerUpdateLauncher(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        updateLauncher = launcher
    }

    /**
     * 업데이트 확인 및 시작
     * @param forceImmediate true면 IMMEDIATE 업데이트 (앱 사용 불가), false면 FLEXIBLE 업데이트
     */
    fun checkForUpdate(forceImmediate: Boolean = true) {
        if (!isActivityResumed()) return
        _updateState.value = UpdateState.Checking

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (!isActivityResumed()) return@addOnSuccessListener
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    val updateType = if (forceImmediate) {
                        AppUpdateType.IMMEDIATE
                    } else {
                        AppUpdateType.FLEXIBLE
                    }

                    if (appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                        startUpdate(appUpdateInfo, updateType)
                    } else {
                        // 요청한 타입이 불가능하면 다른 타입 시도
                        val alternativeType = if (forceImmediate) {
                            AppUpdateType.FLEXIBLE
                        } else {
                            AppUpdateType.IMMEDIATE
                        }
                        if (appUpdateInfo.isUpdateTypeAllowed(alternativeType)) {
                            startUpdate(appUpdateInfo, alternativeType)
                        } else {
                            _updateState.value = UpdateState.NotAvailable
                        }
                    }
                }
                UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                    _updateState.value = UpdateState.NotAvailable
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    // 이미 업데이트가 진행 중
                    startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
                }
                else -> {
                    _updateState.value = UpdateState.NotAvailable
                }
            }
        }.addOnFailureListener { exception ->
            _updateState.value = UpdateState.Failed(exception.message ?: "업데이트 확인 실패")
        }
    }

    private fun isActivityResumed(): Boolean {
        return activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }

    private fun startUpdate(appUpdateInfo: AppUpdateInfo, updateType: Int) {
        if (!isActivityResumed()) {
            _updateState.value = UpdateState.Failed("Activity is not in foreground")
            return
        }
        try {
            val launcher = updateLauncher
            if (launcher != null) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    launcher,
                    AppUpdateOptions.newBuilder(updateType).build()
                )
                _updateState.value = if (updateType == AppUpdateType.IMMEDIATE) {
                    UpdateState.UpdateStarted(isImmediate = true)
                } else {
                    UpdateState.UpdateStarted(isImmediate = false)
                }
            } else {
                _updateState.value = UpdateState.Failed("Update launcher not registered")
            }
        } catch (e: IntentSender.SendIntentException) {
            _updateState.value = UpdateState.Failed(e.message ?: "업데이트 시작 실패")
        } catch (e: IllegalStateException) {
            _updateState.value = UpdateState.Failed(e.message ?: "업데이트 시작 실패")
        }
    }

    /**
     * FLEXIBLE 업데이트 다운로드 완료 후 설치 시작
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    /**
     * onResume에서 호출하여 IMMEDIATE 업데이트가 중단되었는지 확인
     */
    fun checkUpdateOnResume() {
        if (!isActivityResumed()) return
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (!isActivityResumed()) return@addOnSuccessListener
            // IMMEDIATE 업데이트가 중단된 경우 다시 시작
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
            }
            // FLEXIBLE 업데이트가 다운로드 완료된 경우
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                _updateState.value = UpdateState.Downloaded
            }
        }
    }

    /**
     * 업데이트 결과 처리
     * ActivityResultLauncher의 콜백에서 호출
     */
    fun handleUpdateResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                // 업데이트 수락됨
            }
            Activity.RESULT_CANCELED -> {
                _updateState.value = UpdateState.Canceled
            }
            else -> {
                _updateState.value = UpdateState.Failed("업데이트 실패 (code: $resultCode)")
            }
        }
    }

    /**
     * 리스너 해제 - onDestroy에서 호출
     */
    fun unregisterListener() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }
}

/**
 * 업데이트 상태
 */
sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data object NotAvailable : UpdateState()
    data class UpdateStarted(val isImmediate: Boolean) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data object Downloaded : UpdateState()
    data object Installing : UpdateState()
    data object Installed : UpdateState()
    data object Canceled : UpdateState()
    data class Failed(val message: String) : UpdateState()
}
