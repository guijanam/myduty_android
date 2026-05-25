package com.sonbum.diacalendar2.domain.model

data class BoardCategory(
    val code: String,
    val displayName: String,
    val sortOrder: Int = 0,
    val isAnonymous: Boolean = false
) {
    companion object {
        val FREE = BoardCategory("FREE", "자유게시판", 1)

        fun fromCode(code: String, categories: List<BoardCategory>): BoardCategory =
            categories.find { it.code == code } ?: BoardCategory(code, code)
    }
}

data class Post(
    val id: Long,
    val authorId: String,
    val authorNickname: String,
    val category: BoardCategory,
    val title: String,
    val content: String,
    val createdAt: String,
    val commentCount: Int = 0,
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val isLiked: Boolean = false
)

data class Comment(
    val id: Long,
    val postId: Long,
    val parentId: Long?,
    val authorId: String,
    val authorNickname: String,
    val content: String,
    val isDeleted: Boolean,
    val createdAt: String,
    val replies: List<Comment> = emptyList()
)

enum class ReportReason(val code: String, val displayName: String) {
    SPAM("SPAM", "스팸/광고"),
    ABUSE("ABUSE", "욕설/비방"),
    INAPPROPRIATE("INAPPROPRIATE", "부적절한 콘텐츠"),
    OTHER("OTHER", "기타")
}

data class BlockedUser(
    val id: Long,
    val blockedId: String,
    val blockedNickname: String,
    val createdAt: String
)

data class Announcement(
    val id: Long,
    val title: String,
    val content: String,
    val isPinned: Boolean,
    val createdAt: String
)

data class BanStatus(
    val isBanned: Boolean,
    val reason: String = "",
    val bannedUntil: String? = null
)
