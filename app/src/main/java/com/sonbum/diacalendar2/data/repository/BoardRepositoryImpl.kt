package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.remote.BoardSupabaseConfig
import com.sonbum.diacalendar2.data.remote.api.SupabaseBoardApi
import com.sonbum.diacalendar2.data.remote.dto.BlockCreateDto
import com.sonbum.diacalendar2.data.remote.dto.CommentCreateDto
import com.sonbum.diacalendar2.data.remote.dto.IncrementViewCountRequest
import com.sonbum.diacalendar2.data.remote.dto.PostCreateDto
import com.sonbum.diacalendar2.data.remote.dto.PostUpdateDto
import com.sonbum.diacalendar2.data.remote.dto.ReportCreateDto
import com.sonbum.diacalendar2.data.remote.dto.ToggleLikeRequest
import com.sonbum.diacalendar2.domain.model.Announcement
import com.sonbum.diacalendar2.domain.model.BanStatus
import com.sonbum.diacalendar2.domain.model.BlockedUser
import com.sonbum.diacalendar2.domain.model.BoardCategory
import com.sonbum.diacalendar2.domain.model.Comment
import com.sonbum.diacalendar2.domain.model.Post
import java.time.Instant
import com.sonbum.diacalendar2.domain.repository.AuthRepository
import com.sonbum.diacalendar2.domain.repository.BoardRepository
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class BoardRepositoryImpl(
    private val api: SupabaseBoardApi,
    private val authRepository: AuthRepository
) : BoardRepository {

    private val apiKey = BoardSupabaseConfig.apiKey
    private var cachedBlockedUserIds: Set<String> = emptySet()
    private var cachedCategories: List<BoardCategory> = emptyList()

    private suspend fun getAuthHeader(): String {
        val token = authRepository.getAccessToken()
            ?: throw Exception("로그인이 필요합니다")
        return "Bearer $token"
    }

    /**
     * 토큰 만료 시 자동으로 refresh 후 재시도하는 래퍼.
     * 401 에러 발생 시 refreshAccessToken()을 호출하고, 성공하면 block을 재실행한다.
     */
    private suspend fun <T> withAutoRefresh(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            if (e.code() == 401) {
                // 토큰 refresh 시도
                val newToken = authRepository.refreshAccessToken()
                if (newToken != null) {
                    // refresh 성공 → 재시도
                    block()
                } else {
                    throw e
                }
            } else {
                throw e
            }
        }
    }

    private fun mapBoardError(e: Exception): Exception {
        return when {
            e is UnknownHostException || e is java.net.ConnectException ->
                Exception("네트워크에 연결할 수 없습니다. 인터넷 연결을 확인해주세요.")
            e is SocketTimeoutException ->
                Exception("서버 응답이 없습니다. 잠시 후 다시 시도해주세요.")
            e is HttpException && e.code() == 401 ->
                Exception("세션이 만료되었습니다. 다시 로그인해주세요.")
            e is HttpException && e.code() == 403 ->
                Exception("접근 권한이 없습니다.")
            e is HttpException && e.code() in 500..599 ->
                Exception("서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.")
            else -> e
        }
    }

    private fun categoryFromCode(code: String): BoardCategory =
        BoardCategory.fromCode(code, cachedCategories)

    override suspend fun getCategories(): Result<List<BoardCategory>> {
        return try {
            val dtos = withAutoRefresh {
                val auth = getAuthHeader()
                api.getBoardCategories(apiKey = apiKey, authorization = auth)
            }
            val categories = dtos.map { dto ->
                BoardCategory(
                    code = dto.code,
                    displayName = dto.displayName,
                    sortOrder = dto.sortOrder,
                    isAnonymous = dto.isAnonymous
                )
            }
            cachedCategories = categories
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun getPosts(
        category: BoardCategory?,
        searchQuery: String?,
        limit: Int,
        offset: Int
    ): Result<List<Post>> {
        return try {
            val categoryFilter = category?.let { "eq.${it.code}" }
            val orFilter = searchQuery?.takeIf { it.isNotBlank() }?.let { query ->
                "(title.ilike.*$query*,content.ilike.*$query*)"
            }
            val dtos = withAutoRefresh {
                val auth = getAuthHeader()
                api.getPosts(
                    apiKey = apiKey,
                    authorization = auth,
                    categoryFilter = categoryFilter,
                    orFilter = orFilter,
                    limit = limit,
                    offset = offset
                )
            }
            val posts = dtos.map { dto ->
                val cat = categoryFromCode(dto.category)
                Post(
                    id = dto.id,
                    authorId = dto.authorId,
                    authorNickname = if (cat.isAnonymous) "익명" else (dto.profiles?.nickname ?: "익명"),
                    category = cat,
                    title = dto.title,
                    content = dto.content,
                    createdAt = dto.createdAt,
                    commentCount = dto.comments?.firstOrNull()?.count ?: 0,
                    viewCount = dto.viewCount,
                    likeCount = dto.likeCount
                )
            }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun getMyPosts(): Result<List<Post>> {
        return try {
            val auth = getAuthHeader()
            val userId = authRepository.getUserId()
                ?: return Result.failure(Exception("사용자 정보가 없습니다"))
            val dtos = api.getPosts(
                apiKey = apiKey,
                authorization = auth,
                authorIdFilter = "eq.$userId"
            )
            val posts = dtos.map { dto ->
                val cat = categoryFromCode(dto.category)
                Post(
                    id = dto.id,
                    authorId = dto.authorId,
                    authorNickname = if (cat.isAnonymous) "익명" else (dto.profiles?.nickname ?: "익명"),
                    category = cat,
                    title = dto.title,
                    content = dto.content,
                    createdAt = dto.createdAt,
                    commentCount = dto.comments?.firstOrNull()?.count ?: 0,
                    viewCount = dto.viewCount,
                    likeCount = dto.likeCount
                )
            }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun getPostDetail(postId: Long): Result<Post> {
        return try {
            val auth = getAuthHeader()
            val dtos = api.getPostById(
                apiKey = apiKey,
                authorization = auth,
                idFilter = "eq.$postId"
            )
            val dto = dtos.firstOrNull()
                ?: return Result.failure(Exception("게시글을 찾을 수 없습니다"))
            val cat = categoryFromCode(dto.category)
            Result.success(
                Post(
                    id = dto.id,
                    authorId = dto.authorId,
                    authorNickname = if (cat.isAnonymous) "익명" else (dto.profiles?.nickname ?: "익명"),
                    category = cat,
                    title = dto.title,
                    content = dto.content,
                    createdAt = dto.createdAt,
                    viewCount = dto.viewCount,
                    likeCount = dto.likeCount
                )
            )
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun createPost(
        category: BoardCategory,
        title: String,
        content: String
    ): Result<Post> {
        return try {
            val auth = getAuthHeader()
            val userId = authRepository.getUserId()
                ?: return Result.failure(Exception("사용자 정보가 없습니다"))

            val dtos = api.createPost(
                apiKey = apiKey,
                authorization = auth,
                post = PostCreateDto(
                    authorId = userId,
                    category = category.code,
                    title = title,
                    content = content
                )
            )
            val dto = dtos.firstOrNull()
                ?: return Result.failure(Exception("게시글 생성 실패"))
            Result.success(
                Post(
                    id = dto.id,
                    authorId = dto.authorId,
                    authorNickname = dto.profiles?.nickname ?: "익명",
                    category = categoryFromCode(dto.category),
                    title = dto.title,
                    content = dto.content,
                    createdAt = dto.createdAt
                )
            )
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun updatePost(
        postId: Long,
        category: BoardCategory,
        title: String,
        content: String
    ): Result<Post> {
        return try {
            val auth = getAuthHeader()
            val dtos = api.updatePost(
                apiKey = apiKey,
                authorization = auth,
                idFilter = "eq.$postId",
                post = PostUpdateDto(
                    category = category.code,
                    title = title,
                    content = content
                )
            )
            val dto = dtos.firstOrNull()
                ?: return Result.failure(Exception("게시글 수정 실패"))
            Result.success(
                Post(
                    id = dto.id,
                    authorId = dto.authorId,
                    authorNickname = dto.profiles?.nickname ?: "익명",
                    category = categoryFromCode(dto.category),
                    title = dto.title,
                    content = dto.content,
                    createdAt = dto.createdAt
                )
            )
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun deletePost(postId: Long): Result<Unit> {
        return try {
            val auth = getAuthHeader()
            api.deletePost(apiKey = apiKey, authorization = auth, idFilter = "eq.$postId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    // ===== 조회수 & 좋아요 =====

    override suspend fun incrementViewCount(postId: Long): Result<Unit> {
        return try {
            val auth = getAuthHeader()
            val userId = authRepository.getUserId() ?: return Result.success(Unit)
            api.incrementViewCount(
                apiKey = apiKey,
                authorization = auth,
                body = IncrementViewCountRequest(postId = postId, userId = userId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun toggleLike(postId: Long): Result<Boolean> {
        return try {
            val auth = getAuthHeader()
            val userId = authRepository.getUserId()
                ?: return Result.failure(Exception("사용자 정보가 없습니다"))
            val isLiked = api.togglePostLike(
                apiKey = apiKey,
                authorization = auth,
                body = ToggleLikeRequest(postId = postId, userId = userId)
            )
            Result.success(isLiked)
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun isPostLiked(postId: Long): Result<Boolean> {
        return try {
            val auth = getAuthHeader()
            val userId = authRepository.getUserId()
                ?: return Result.success(false)
            val likes = api.getPostLikeStatus(
                apiKey = apiKey,
                authorization = auth,
                postIdFilter = "eq.$postId",
                userIdFilter = "eq.$userId"
            )
            Result.success(likes.isNotEmpty())
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun getComments(postId: Long, isAnonymous: Boolean): Result<List<Comment>> {
        return try {
            val auth = getAuthHeader()
            val dtos = api.getComments(
                apiKey = apiKey,
                authorization = auth,
                postIdFilter = "eq.$postId"
            )
            val allComments = dtos.map { dto ->
                Comment(
                    id = dto.id,
                    postId = dto.postId,
                    parentId = dto.parentId,
                    authorId = dto.authorId,
                    authorNickname = if (isAnonymous) "익명" else (dto.profiles?.nickname ?: "익명"),
                    content = dto.content,
                    isDeleted = dto.isDeleted,
                    createdAt = dto.createdAt
                )
            }

            // 차단된 사용자 댓글 필터링
            val filtered = allComments.filter { it.authorId !in cachedBlockedUserIds }

            // 1단계 대댓글 계층 구조 구성
            val topLevel = filtered.filter { it.parentId == null }
            val grouped = filtered.filter { it.parentId != null }.groupBy { it.parentId }

            val nested = topLevel.map { parent ->
                parent.copy(replies = grouped[parent.id] ?: emptyList())
            }
            Result.success(nested)
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun createComment(
        postId: Long,
        parentId: Long?,
        content: String
    ): Result<Comment> {
        return try {
            val auth = getAuthHeader()
            val userId = authRepository.getUserId()
                ?: return Result.failure(Exception("사용자 정보가 없습니다"))

            val dtos = api.createComment(
                apiKey = apiKey,
                authorization = auth,
                comment = CommentCreateDto(
                    postId = postId,
                    parentId = parentId,
                    authorId = userId,
                    content = content
                )
            )
            val dto = dtos.firstOrNull()
                ?: return Result.failure(Exception("댓글 생성 실패"))
            Result.success(
                Comment(
                    id = dto.id,
                    postId = dto.postId,
                    parentId = dto.parentId,
                    authorId = dto.authorId,
                    authorNickname = dto.profiles?.nickname ?: "익명",
                    content = dto.content,
                    isDeleted = dto.isDeleted,
                    createdAt = dto.createdAt
                )
            )
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun deleteComment(commentId: Long): Result<Unit> {
        return try {
            val auth = getAuthHeader()
            api.softDeleteComment(
                apiKey = apiKey,
                authorization = auth,
                idFilter = "eq.$commentId",
                body = mapOf("is_deleted" to true)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    // ===== 신고 =====

    override suspend fun reportPost(postId: Long, targetAuthorId: String, reason: String): Result<Unit> {
        return try {
            val auth = getAuthHeader()
            val userId = authRepository.getUserId()
                ?: return Result.failure(Exception("사용자 정보가 없습니다"))
            api.createReport(
                apiKey = apiKey,
                authorization = auth,
                report = ReportCreateDto(
                    reporterId = userId,
                    contentType = "post",
                    contentId = postId,
                    targetAuthorId = targetAuthorId,
                    reason = reason
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is HttpException && e.code() == 409) {
                return Result.failure(Exception("이미 신고한 게시글입니다"))
            }
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun reportComment(commentId: Long, targetAuthorId: String, reason: String): Result<Unit> {
        return try {
            val auth = getAuthHeader()
            val userId = authRepository.getUserId()
                ?: return Result.failure(Exception("사용자 정보가 없습니다"))
            api.createReport(
                apiKey = apiKey,
                authorization = auth,
                report = ReportCreateDto(
                    reporterId = userId,
                    contentType = "comment",
                    contentId = commentId,
                    targetAuthorId = targetAuthorId,
                    reason = reason
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is HttpException && e.code() == 409) {
                return Result.failure(Exception("이미 신고한 댓글입니다"))
            }
            Result.failure(mapBoardError(e))
        }
    }

    // ===== 차단 =====

    override suspend fun blockUser(blockedId: String): Result<Unit> {
        return try {
            val auth = getAuthHeader()
            val userId = authRepository.getUserId()
                ?: return Result.failure(Exception("사용자 정보가 없습니다"))
            api.createBlock(
                apiKey = apiKey,
                authorization = auth,
                block = BlockCreateDto(blockerId = userId, blockedId = blockedId)
            )
            cachedBlockedUserIds = cachedBlockedUserIds + blockedId
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is HttpException && e.code() == 409) {
                return Result.failure(Exception("이미 차단한 사용자입니다"))
            }
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun unblockUser(blockedId: String): Result<Unit> {
        return try {
            val auth = getAuthHeader()
            val userId = authRepository.getUserId()
                ?: return Result.failure(Exception("사용자 정보가 없습니다"))
            api.deleteBlock(
                apiKey = apiKey,
                authorization = auth,
                blockerIdFilter = "eq.$userId",
                blockedIdFilter = "eq.$blockedId"
            )
            cachedBlockedUserIds = cachedBlockedUserIds - blockedId
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun getBlockedUserIds(): Result<Set<String>> {
        return try {
            val userId = authRepository.getUserId()
                ?: return Result.failure(Exception("사용자 정보가 없습니다"))
            val blocks = withAutoRefresh {
                val auth = getAuthHeader()
                api.getBlocks(
                    apiKey = apiKey,
                    authorization = auth,
                    blockerIdFilter = "eq.$userId"
                )
            }
            val ids = blocks.map { it.blockedId }.toSet()
            cachedBlockedUserIds = ids
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    override suspend fun getBlockedUsers(): Result<List<BlockedUser>> {
        return try {
            val auth = getAuthHeader()
            val userId = authRepository.getUserId()
                ?: return Result.failure(Exception("사용자 정보가 없습니다"))
            val blocks = api.getBlocks(
                apiKey = apiKey,
                authorization = auth,
                blockerIdFilter = "eq.$userId"
            )
            val blockedUsers = blocks.map { dto ->
                BlockedUser(
                    id = dto.id,
                    blockedId = dto.blockedId,
                    blockedNickname = dto.profiles?.nickname ?: "알 수 없음",
                    createdAt = dto.createdAt
                )
            }
            cachedBlockedUserIds = blocks.map { it.blockedId }.toSet()
            Result.success(blockedUsers)
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    // ===== 공지사항 =====

    override suspend fun getAnnouncements(): Result<List<Announcement>> {
        return try {
            val dtos = withAutoRefresh {
                val auth = getAuthHeader()
                api.getAnnouncements(apiKey = apiKey, authorization = auth)
            }
            val announcements = dtos.map { dto ->
                Announcement(
                    id = dto.id,
                    title = dto.title,
                    content = dto.content,
                    isPinned = dto.isPinned,
                    createdAt = dto.createdAt
                )
            }
            Result.success(announcements)
        } catch (e: Exception) {
            Result.failure(mapBoardError(e))
        }
    }

    // ===== 새 글 확인 =====

    override suspend fun hasNewPosts(since: String): Result<Boolean> {
        return try {
            val auth = getAuthHeader()
            val response = api.hasNewPosts(
                apiKey = apiKey,
                authorization = auth,
                createdAtFilter = "gt.$since"
            )
            val contentRange = response.headers()["content-range"]
            val count = contentRange?.substringAfter("/")?.toIntOrNull() ?: 0
            Result.success(count > 0)
        } catch (_: Exception) {
            Result.success(false)
        }
    }

    // ===== 사용자 정지 확인 =====

    override suspend fun checkBanStatus(): Result<BanStatus> {
        return try {
            val userId = authRepository.getUserId()
                ?: return Result.success(BanStatus(isBanned = false))
            val bans = withAutoRefresh {
                val auth = getAuthHeader()
                api.getBannedUser(
                    apiKey = apiKey,
                    authorization = auth,
                    userIdFilter = "eq.$userId"
                )
            }
            val ban = bans.firstOrNull()
            if (ban == null) {
                Result.success(BanStatus(isBanned = false))
            } else {
                val isActive = ban.bannedUntil == null ||
                        try { Instant.parse(ban.bannedUntil).isAfter(Instant.now()) } catch (_: Exception) { true }
                Result.success(BanStatus(isBanned = isActive, reason = ban.reason, bannedUntil = ban.bannedUntil))
            }
        } catch (e: Exception) {
            Result.success(BanStatus(isBanned = false))
        }
    }
}
