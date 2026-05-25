package com.sonbum.diacalendar2.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    val currentUserId: Flow<String?>
    val currentNickname: Flow<String?>

    /** Google ID Token으로 Supabase 로그인. Result<Boolean> = 닉네임 존재 여부 */
    suspend fun signInWithGoogle(googleIdToken: String): Result<Boolean>
    suspend fun signOut()
    suspend fun setNickname(nickname: String): Result<Unit>
    /** 닉네임 유효성 검사 (금칙어 + 중복). null=유효, String=에러메시지 */
    suspend fun checkNickname(nickname: String): Result<String?>
    suspend fun getAccessToken(): String?
    suspend fun refreshAccessToken(): String?
    suspend fun getUserId(): String?
}
