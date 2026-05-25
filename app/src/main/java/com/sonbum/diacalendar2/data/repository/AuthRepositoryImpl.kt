package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.datastore.AuthPreferences
import com.sonbum.diacalendar2.data.remote.BoardSupabaseConfig
import com.sonbum.diacalendar2.data.remote.api.SupabaseBoardApi
import com.sonbum.diacalendar2.data.remote.dto.CheckNicknameRequest
import com.sonbum.diacalendar2.data.remote.dto.ProfileUpsertDto
import com.sonbum.diacalendar2.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import android.util.Log
import org.json.JSONObject
import retrofit2.HttpException

class AuthRepositoryImpl(
    private val api: SupabaseBoardApi,
    private val authPreferences: AuthPreferences
) : AuthRepository {

    private val apiKey = BoardSupabaseConfig.apiKey

    override val isLoggedIn: Flow<Boolean> = authPreferences.isLoggedIn
    override val currentUserId: Flow<String?> = authPreferences.userId
    override val currentNickname: Flow<String?> = authPreferences.nickname

    override suspend fun signInWithGoogle(googleIdToken: String): Result<Boolean> {
        return try {
            Log.d("AuthRepo", "=== signInWithGoogle 시작 ===")
            Log.d("AuthRepo", "idToken 길이: ${googleIdToken.length}")
            Log.d("AuthRepo", "apiKey: ${apiKey.take(10)}...")
            val response = api.signInWithIdToken(
                apiKey = apiKey,
                body = mapOf("provider" to "google", "id_token" to googleIdToken)
            )
            Log.d("AuthRepo", "Supabase 응답 수신")
            Log.d("AuthRepo", "user: ${response.user?.id}, email: ${response.user?.email}")
            Log.d("AuthRepo", "accessToken 존재: ${response.accessToken != null}, 길이: ${response.accessToken?.length}")
            Log.d("AuthRepo", "refreshToken 존재: ${response.refreshToken != null}")
            val user = response.user ?: return Result.failure(Exception("로그인 실패: user가 null"))
            val token = response.accessToken ?: return Result.failure(Exception("로그인 실패: accessToken이 null"))
            authPreferences.saveAuthSession(
                accessToken = token,
                refreshToken = response.refreshToken,
                userId = user.id,
                email = user.email
            )
            Log.d("AuthRepo", "세션 저장 완료")
            // 프로필에서 닉네임 로드
            var hasNickname = false
            try {
                Log.d("AuthRepo", "프로필 닉네임 조회 시작")
                val profiles = api.getProfile(
                    apiKey = apiKey,
                    authorization = "Bearer $token",
                    idFilter = "eq.${user.id}"
                )
                Log.d("AuthRepo", "프로필 조회 결과: ${profiles.size}개")
                profiles.firstOrNull()?.let {
                    Log.d("AuthRepo", "닉네임: '${it.nickname}'")
                    if (it.nickname.isNotBlank()) {
                        authPreferences.saveNickname(it.nickname)
                        hasNickname = true
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthRepo", "프로필 조회 실패", e)
            }
            Log.d("AuthRepo", "=== signInWithGoogle 완료: hasNickname=$hasNickname ===")
            Result.success(hasNickname)
        } catch (e: HttpException) {
            val errorMsg = parseAuthError(e)
            Log.e("AuthRepo", "HTTP 에러: code=${e.code()}, message=$errorMsg", e)
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Log.e("AuthRepo", "네트워크 에러: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(Exception("네트워크 오류가 발생했습니다"))
        }
    }

    private fun parseAuthError(e: HttpException): String {
        return try {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            val json = JSONObject(errorBody)
            val errorDescription = json.optString("error_description", "")
            val msg = json.optString("msg", "")
            val error = json.optString("error", "")
            (errorDescription.ifBlank { msg.ifBlank { error } }).ifBlank { "오류가 발생했습니다" }
        } catch (_: Exception) {
            "오류가 발생했습니다"
        }
    }

    override suspend fun signOut() {
        try {
            val token = authPreferences.accessToken.first()
            if (token != null) {
                api.signOut(apiKey, "Bearer $token")
            }
        } catch (_: Exception) {
            // 서버 로그아웃 실패해도 로컬 세션은 삭제
        }
        authPreferences.clearSession()
    }

    override suspend fun checkNickname(nickname: String): Result<String?> {
        return try {
            val token = authPreferences.accessToken.first()
                ?: return Result.failure(Exception("로그인이 필요합니다"))
            val userId = authPreferences.userId.first()
                ?: return Result.failure(Exception("사용자 정보가 없습니다"))

            val response = api.checkNickname(
                apiKey = apiKey,
                authorization = "Bearer $token",
                body = CheckNicknameRequest(nickname = nickname, userId = userId)
            )

            if (response.valid) {
                Result.success(null)
            } else {
                val message = when (response.reason) {
                    "banned_word" -> "사용할 수 없는 닉네임입니다"
                    "duplicate" -> "이미 사용 중인 닉네임입니다"
                    else -> "사용할 수 없는 닉네임입니다"
                }
                Result.success(message)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setNickname(nickname: String): Result<Unit> {
        return try {
            val token = authPreferences.accessToken.first()
                ?: return Result.failure(Exception("로그인이 필요합니다"))
            val userId = authPreferences.userId.first()
                ?: return Result.failure(Exception("사용자 정보가 없습니다"))

            // 닉네임 검증 (금칙어 + 중복)
            val checkResult = checkNickname(nickname).getOrNull()
            if (checkResult != null) {
                return Result.failure(Exception(checkResult))
            }

            api.upsertProfile(
                apiKey = apiKey,
                authorization = "Bearer $token",
                profile = ProfileUpsertDto(id = userId, nickname = nickname)
            )
            authPreferences.saveNickname(nickname)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAccessToken(): String? {
        return authPreferences.accessToken.first()
    }

    override suspend fun refreshAccessToken(): String? {
        return try {
            val refreshToken = authPreferences.refreshToken.first() ?: return null
            val response = api.refreshToken(
                apiKey = apiKey,
                body = mapOf("refresh_token" to refreshToken)
            )
            val newAccessToken = response.accessToken ?: return null
            val userId = response.user?.id ?: authPreferences.userId.first() ?: ""
            val email = response.user?.email
            authPreferences.saveAuthSession(
                accessToken = newAccessToken,
                refreshToken = response.refreshToken ?: refreshToken,
                userId = userId,
                email = email
            )
            newAccessToken
        } catch (e: Exception) {
            Log.e("AuthRepository", "토큰 갱신 실패", e)
            null
        }
    }

    override suspend fun getUserId(): String? {
        return authPreferences.userId.first()
    }
}
