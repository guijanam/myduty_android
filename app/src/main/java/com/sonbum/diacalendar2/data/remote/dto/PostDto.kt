package com.sonbum.diacalendar2.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PostDto(
    val id: Long,
    @SerializedName("author_id")
    val authorId: String,
    val category: String,
    val title: String,
    val content: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("view_count")
    val viewCount: Int = 0,
    @SerializedName("like_count")
    val likeCount: Int = 0,
    val profiles: ProfileDto?,
    val comments: List<CommentCountDto>?
)

data class CommentCountDto(
    val count: Int
)

data class PostCreateDto(
    @SerializedName("author_id")
    val authorId: String,
    val category: String,
    val title: String,
    val content: String
)

data class PostUpdateDto(
    val category: String,
    val title: String,
    val content: String
)

data class CommentDto(
    val id: Long,
    @SerializedName("post_id")
    val postId: Long,
    @SerializedName("parent_id")
    val parentId: Long?,
    @SerializedName("author_id")
    val authorId: String,
    val content: String,
    @SerializedName("is_deleted")
    val isDeleted: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    val profiles: ProfileDto?
)

data class CommentCreateDto(
    @SerializedName("post_id")
    val postId: Long,
    @SerializedName("parent_id")
    val parentId: Long?,
    @SerializedName("author_id")
    val authorId: String,
    val content: String
)

data class ReportCreateDto(
    @SerializedName("reporter_id")
    val reporterId: String,
    @SerializedName("content_type")
    val contentType: String,
    @SerializedName("content_id")
    val contentId: Long,
    @SerializedName("target_author_id")
    val targetAuthorId: String,
    val reason: String
)

data class BlockCreateDto(
    @SerializedName("blocker_id")
    val blockerId: String,
    @SerializedName("blocked_id")
    val blockedId: String
)

data class BlockDto(
    val id: Long,
    @SerializedName("blocker_id")
    val blockerId: String,
    @SerializedName("blocked_id")
    val blockedId: String,
    @SerializedName("created_at")
    val createdAt: String,
    val profiles: ProfileDto?
)

data class AnnouncementDto(
    val id: Long,
    val title: String,
    val content: String,
    @SerializedName("is_pinned")
    val isPinned: Boolean,
    @SerializedName("created_at")
    val createdAt: String
)

data class BoardCategoryDto(
    val id: Long,
    val code: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("sort_order")
    val sortOrder: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("is_anonymous")
    val isAnonymous: Boolean = false
)

data class IncrementViewCountRequest(
    @SerializedName("p_post_id")
    val postId: Long,
    @SerializedName("p_user_id")
    val userId: String
)

data class ToggleLikeRequest(
    @SerializedName("p_post_id")
    val postId: Long,
    @SerializedName("p_user_id")
    val userId: String
)

data class PostLikeDto(
    val id: Long,
    @SerializedName("post_id")
    val postId: Long,
    @SerializedName("user_id")
    val userId: String
)

data class CheckNicknameRequest(
    @SerializedName("p_nickname")
    val nickname: String,
    @SerializedName("p_user_id")
    val userId: String
)

data class CheckNicknameResponse(
    val valid: Boolean,
    val reason: String?
)

data class BannedUserDto(
    val id: Long,
    @SerializedName("user_id")
    val userId: String,
    val reason: String,
    @SerializedName("banned_until")
    val bannedUntil: String?
)
