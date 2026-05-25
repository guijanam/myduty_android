package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.Announcement
import com.sonbum.diacalendar2.domain.model.BanStatus
import com.sonbum.diacalendar2.domain.model.BlockedUser
import com.sonbum.diacalendar2.domain.model.BoardCategory
import com.sonbum.diacalendar2.domain.model.Comment
import com.sonbum.diacalendar2.domain.model.Post

interface BoardRepository {
    // 카테고리
    suspend fun getCategories(): Result<List<BoardCategory>>

    suspend fun getPosts(
        category: BoardCategory? = null,
        searchQuery: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<Post>>
    suspend fun getMyPosts(): Result<List<Post>>
    suspend fun getPostDetail(postId: Long): Result<Post>
    suspend fun createPost(category: BoardCategory, title: String, content: String): Result<Post>
    suspend fun updatePost(postId: Long, category: BoardCategory, title: String, content: String): Result<Post>
    suspend fun deletePost(postId: Long): Result<Unit>

    // 조회수 & 좋아요
    suspend fun incrementViewCount(postId: Long): Result<Unit>
    suspend fun toggleLike(postId: Long): Result<Boolean>
    suspend fun isPostLiked(postId: Long): Result<Boolean>

    suspend fun getComments(postId: Long, isAnonymous: Boolean = false): Result<List<Comment>>
    suspend fun createComment(postId: Long, parentId: Long?, content: String): Result<Comment>
    suspend fun deleteComment(commentId: Long): Result<Unit>

    // 신고
    suspend fun reportPost(postId: Long, targetAuthorId: String, reason: String): Result<Unit>
    suspend fun reportComment(commentId: Long, targetAuthorId: String, reason: String): Result<Unit>

    // 차단
    suspend fun blockUser(blockedId: String): Result<Unit>
    suspend fun unblockUser(blockedId: String): Result<Unit>
    suspend fun getBlockedUserIds(): Result<Set<String>>
    suspend fun getBlockedUsers(): Result<List<BlockedUser>>

    // 공지사항
    suspend fun getAnnouncements(): Result<List<Announcement>>

    // 사용자 정지 확인
    suspend fun checkBanStatus(): Result<BanStatus>

    // 새 글 확인
    suspend fun hasNewPosts(since: String): Result<Boolean>
}
