package com.sonbum.diacalendar2.presentation.board

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CommunityGuidelinesDialog(
    onAgree: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("커뮤니티 이용 약관") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "게시판을 이용하기 전에 아래 커뮤니티 가이드라인에 동의해주세요.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                GuidelineItem("1. 금지 행위", """
                    • 타인에 대한 비방, 욕설, 모욕적 표현
                    • 음란물 또는 선정적 콘텐츠 게시
                    • 스팸, 광고, 도배 행위
                    • 개인정보 무단 공개
                    • 허위 정보 유포
                """.trimIndent())

                GuidelineItem("2. 권한 및 책임", """
                    • 게시물로 인해 발생하는 법적 책임은 작성자에게 있습니다.
                    • 타인의 저작물을 무단으로 게시하지 마세요.
                """.trimIndent())

                GuidelineItem("3. 개인정보", """
                    • 회원가입 시 제공한 이메일은 계정 관리 목적으로만 사용됩니다.
                    • 닉네임은 게시판에서 공개적으로 표시됩니다.
                """.trimIndent())

                GuidelineItem("4. 제재 조치", """
                    • 가이드라인 위반 시 게시물이 삭제될 수 있습니다.
                    • 반복적인 위반 시 계정 이용이 제한될 수 있습니다.
                    • 신고된 콘텐츠는 관리자가 검토 후 조치합니다.
                """.trimIndent())

                GuidelineItem("5. 신고 및 차단", """
                    • 부적절한 게시글이나 댓글은 신고할 수 있습니다.
                    • 특정 사용자를 차단하여 해당 사용자의 콘텐츠를 숨길 수 있습니다.
                """.trimIndent())
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text("동의합니다")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun GuidelineItem(title: String, content: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = content,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
