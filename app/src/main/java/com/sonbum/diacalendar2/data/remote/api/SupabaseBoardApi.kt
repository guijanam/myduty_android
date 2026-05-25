package com.sonbum.diacalendar2.data.remote.api

import com.sonbum.diacalendar2.data.remote.dto.AuthResponse
import com.sonbum.diacalendar2.data.remote.dto.AuthUser
import com.sonbum.diacalendar2.data.remote.dto.PasswordRecoverRequest
import com.sonbum.diacalendar2.data.remote.dto.PasswordUpdateRequest
import com.sonbum.diacalendar2.data.remote.dto.AnnouncementDto
import com.sonbum.diacalendar2.data.remote.dto.BannedUserDto
import com.sonbum.diacalendar2.data.remote.dto.BoardCategoryDto
import com.sonbum.diacalendar2.data.remote.dto.CheckNicknameRequest
import com.sonbum.diacalendar2.data.remote.dto.CheckNicknameResponse
import com.sonbum.diacalendar2.data.remote.dto.BlockCreateDto
import com.sonbum.diacalendar2.data.remote.dto.BlockDto
import com.sonbum.diacalendar2.data.remote.dto.CommentCreateDto
import com.sonbum.diacalendar2.data.remote.dto.CommentDto
import com.sonbum.diacalendar2.data.remote.dto.IncrementViewCountRequest
import com.sonbum.diacalendar2.data.remote.dto.PostCreateDto
import com.sonbum.diacalendar2.data.remote.dto.PostDto
import com.sonbum.diacalendar2.data.remote.dto.PostLikeDto
import com.sonbum.diacalendar2.data.remote.dto.PostUpdateDto
import com.sonbum.diacalendar2.data.remote.dto.ToggleLikeRequest
import com.sonbum.diacalendar2.data.remote.dto.ProfileDto
import com.sonbum.diacalendar2.data.remote.dto.ProfileUpsertDto
import com.sonbum.diacalendar2.data.remote.dto.ReportCreateDto
import com.sonbum.diacalendar2.data.remote.dto.SignInRequest
import com.sonbum.diacalendar2.data.remote.dto.SignUpRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface SupabaseBoardApi {

    // ===== Auth =====

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Body request: SignUpRequest
    ): Response<ResponseBody>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Body request: SignInRequest
    ): AuthResponse

    @POST("auth/v1/token?grant_type=id_token")
    suspend fun signInWithIdToken(
        @Header("apikey") apiKey: String,
        @Body body: Map<String, String>
    ): AuthResponse

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Header("apikey") apiKey: String,
        @Body body: Map<String, String>
    ): AuthResponse

    @POST("auth/v1/logout")
    suspend fun signOut(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    )

    @GET("auth/v1/user")
    suspend fun getUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): AuthUser

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Header("apikey") apiKey: String,
        @Body request: PasswordRecoverRequest
    )

    @PUT("auth/v1/user")
    suspend fun updateUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body request: PasswordUpdateRequest
    ): AuthUser

    // ===== Profiles =====

    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): List<ProfileDto>

    @POST("rest/v1/profiles")
    suspend fun upsertProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
        @Body profile: ProfileUpsertDto
    ): List<ProfileDto>

    // ===== Posts =====

    @GET("rest/v1/posts")
    suspend fun getPosts(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "id,author_id,title,content,category,created_at,view_count,like_count,profiles(nickname),comments(count)",
        @Query("category") categoryFilter: String? = null,
        @Query("author_id") authorIdFilter: String? = null,
        @Query("or") orFilter: String? = null,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): List<PostDto>

    @GET("rest/v1/posts")
    suspend fun getPostById(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Query("select") select: String = "id,author_id,title,content,category,created_at,view_count,like_count,profiles(nickname)"
    ): List<PostDto>

    @POST("rest/v1/posts")
    suspend fun createPost(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body post: PostCreateDto
    ): List<PostDto>

    @PATCH("rest/v1/posts")
    suspend fun updatePost(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") idFilter: String,
        @Body post: PostUpdateDto
    ): List<PostDto>

    @DELETE("rest/v1/posts")
    suspend fun deletePost(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String
    )

    // ===== View Count & Likes =====

    @POST("rest/v1/rpc/increment_view_count")
    suspend fun incrementViewCount(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: IncrementViewCountRequest
    )

    @POST("rest/v1/rpc/toggle_post_like")
    suspend fun togglePostLike(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: ToggleLikeRequest
    ): Boolean

    @GET("rest/v1/post_likes")
    suspend fun getPostLikeStatus(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("post_id") postIdFilter: String,
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "id,post_id,user_id"
    ): List<PostLikeDto>

    // ===== Comments =====

    @GET("rest/v1/comments")
    suspend fun getComments(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("post_id") postIdFilter: String,
        @Query("select") select: String = "*,profiles(nickname)",
        @Query("order") order: String = "created_at.asc"
    ): List<CommentDto>

    @POST("rest/v1/comments")
    suspend fun createComment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body comment: CommentCreateDto
    ): List<CommentDto>

    @PATCH("rest/v1/comments")
    suspend fun softDeleteComment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Body body: Map<String, Boolean>
    )

    // ===== Reports =====

    @POST("rest/v1/reports")
    suspend fun createReport(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body report: ReportCreateDto
    )

    // ===== Blocks =====

    @GET("rest/v1/blocks")
    suspend fun getBlocks(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("blocker_id") blockerIdFilter: String,
        @Query("select") select: String = "id,blocker_id,blocked_id,created_at,profiles:blocked_id(nickname)"
    ): List<BlockDto>

    @POST("rest/v1/blocks")
    suspend fun createBlock(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body block: BlockCreateDto
    )

    @DELETE("rest/v1/blocks")
    suspend fun deleteBlock(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("blocker_id") blockerIdFilter: String,
        @Query("blocked_id") blockedIdFilter: String
    )

    // ===== Announcements =====

    @GET("rest/v1/announcements")
    suspend fun getAnnouncements(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "id,title,content,is_pinned,created_at",
        @Query("order") order: String = "is_pinned.desc,created_at.desc",
        @Query("limit") limit: Int = 10
    ): List<AnnouncementDto>

    // ===== Banned Users =====

    @GET("rest/v1/banned_users")
    suspend fun getBannedUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "id,user_id,reason,banned_until"
    ): List<BannedUserDto>

    // ===== Nickname Validation =====

    @POST("rest/v1/rpc/check_nickname")
    suspend fun checkNickname(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: CheckNicknameRequest
    ): CheckNicknameResponse

    // ===== New Posts Check =====

    @HEAD("rest/v1/posts")
    suspend fun hasNewPosts(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "count=exact",
        @Query("created_at") createdAtFilter: String,
        @Query("limit") limit: Int = 1
    ): Response<Unit>

    // ===== Board Categories =====

    @GET("rest/v1/board_categories")
    suspend fun getBoardCategories(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("is_active") isActiveFilter: String = "eq.true",
        @Query("select") select: String = "id,code,display_name,sort_order,is_active,is_anonymous",
        @Query("order") order: String = "sort_order.asc"
    ): List<BoardCategoryDto>
}
