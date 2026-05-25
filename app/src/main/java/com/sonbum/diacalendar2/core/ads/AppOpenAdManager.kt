package com.sonbum.diacalendar2.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.sonbum.diacalendar2.R
import java.util.Date

private const val TAG = "AppOpenAdManager"

// 광고 만료 시간: 4시간 (AppOpenAd는 4시간 이내에 노출되어야 유효)
private const val AD_EXPIRY_MS = 4 * 60 * 60 * 1000L

class AppOpenAdManager(private val context: Context) {

    private var appOpenAd: AppOpenAd? = null
    private var loadTime: Long = 0L
    private var isLoadingAd = false
    var isShowingAd = false
        private set

    // 광고 로드 완료 후 즉시 표시할 Activity (loadAndShow 시 사용)
    private var pendingActivity: Activity? = null

    /** 광고 로드 */
    fun loadAd() {
        //Log.d(TAG, "loadAd() 호출 - isLoadingAd=$isLoadingAd, isAdAvailable=${isAdAvailable()}")
        if (isLoadingAd || isAdAvailable()) {
            //Log.d(TAG, "로드 스킵 - isLoadingAd=$isLoadingAd, isAdAvailable=${isAdAvailable()}")
            return
        }

        isLoadingAd = true
        val adUnitId = context.getString(R.string.admob_app_open_ad_unit_id)
        //Log.d(TAG, "광고 로드 시작 - adUnitId=$adUnitId")
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            context,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    //Log.d(TAG, "✅ 광고 로드 완료")
                    appOpenAd = ad
                    loadTime = Date().time
                    isLoadingAd = false

                    // loadAndShow로 요청된 Activity가 있으면 즉시 표시
                    pendingActivity?.let { activity ->
                        //Log.d(TAG, "pendingActivity 있음 → 즉시 광고 표시")
                        pendingActivity = null
                        showAd(activity)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    //Log.e(TAG, "❌ 광고 로드 실패 - code=${error.code}, message=${error.message}, domain=${error.domain}")
                    isLoadingAd = false
                    pendingActivity = null
                }
            }
        )
    }

    /**
     * 광고가 준비됐으면 즉시 표시, 없으면 로드 후 완료 시 표시.
     * 콜드 스타트(MainActivity.onCreate)에서 호출.
     */
    fun loadAndShow(activity: Activity) {
        //Log.d(TAG, "loadAndShow() 호출 - isAdAvailable=${isAdAvailable()}, isShowingAd=$isShowingAd")

        if (isShowingAd) {
            //Log.d(TAG, "이미 광고 표시 중 → 스킵")
            return
        }

        if (isAdAvailable()) {
            //Log.d(TAG, "광고 준비됨 → 즉시 표시")
            showAd(activity)
        } else {
            //Log.d(TAG, "광고 미준비 → 로드 후 표시 예약")
            pendingActivity = activity
            loadAd()
        }
    }

    /** 광고 노출 (이미 로드된 광고가 있을 때만 호출) */
    private fun showAd(activity: Activity) {
        //Log.d(TAG, "showAd() 호출")
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                //Log.d(TAG, "광고 닫힘")
                appOpenAd = null
                isShowingAd = false
                loadAd() // 다음 번을 위해 미리 로드
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
               //Log.e(TAG, "❌ 광고 표시 실패 - code=${error.code}, message=${error.message}")
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                //Log.d(TAG, "✅ 광고 표시됨")
                isShowingAd = true
            }
        }
        appOpenAd?.show(activity)
    }

    /** @deprecated loadAndShow() 사용 권장 */
    fun showAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit = {}) {
        loadAndShow(activity)
    }

    /** 광고가 사용 가능한지 확인 (null 아님 + 만료 전) */
    private fun isAdAvailable(): Boolean {
        val available = appOpenAd != null && !isAdExpired()
        //Log.d(TAG, "isAdAvailable=$available (adNull=${appOpenAd == null}, expired=${isAdExpired()})")
        return available
    }

    private fun isAdExpired(): Boolean {
        val elapsed = Date().time - loadTime
        return elapsed > AD_EXPIRY_MS
    }
}
