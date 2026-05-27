package com.sonbum.diacalendar2.presentation.notifications


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sonbum.diacalendar2.presentation.board.BoardListScreen
import com.sonbum.diacalendar2.presentation.board.MyPostsScreen



@Composable
fun NotificationsScreen(
    onNavigateToPostDetail: (Long) -> Unit = {},
    onNavigateToPostWrite: (String?) -> Unit = {},
    onNavigateToPostEdit: (Long) -> Unit = {},
    onNavigateToDocumentDetail: (String) -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    onNavigateToBlockedUsers: () -> Unit = {},
    boardRefreshTrigger: Int = 0,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
//            Tab(
//                selected = selectedTab == 0,
//                onClick = { selectedTab = 0 },
//                text = { Text("게시판") }
//            )
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("공지") }
            )
//            Tab(
//                selected = selectedTab == 2,
//                onClick = { selectedTab = 2 },
//                text = { Text("내 글 관리") }
//            )
        }

        when (selectedTab) {
//            0 -> BoardListScreen(
//                onNavigateToPostDetail = onNavigateToPostDetail,
//                onNavigateToPostWrite = onNavigateToPostWrite,
//                onNavigateToAuth = onNavigateToAuth,
//                onNavigateToBlockedUsers = onNavigateToBlockedUsers,
//                boardRefreshTrigger = boardRefreshTrigger,
//                modifier = Modifier.weight(1f)
//            )
//            1 -> DocumentListScreen(
//                onNavigateToDetail = onNavigateToDocumentDetail,
//                modifier = Modifier.weight(1f)
//            )
//            2 -> MyPostsScreen(
//                onNavigateToPostDetail = onNavigateToPostDetail,
//                onNavigateToPostEdit = onNavigateToPostEdit,
//                onNavigateToAuth = onNavigateToAuth,
//                modifier = Modifier.weight(1f)
//            )

	        0 -> DocumentListScreen(
		        onNavigateToDetail = onNavigateToDocumentDetail,
		        modifier = Modifier.weight(1f)
	        )

        }
    }
}


