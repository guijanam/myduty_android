package com.sonbum.diacalendar2.presentation.localdia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LocalDiaEditScreen(
    officeId: Long,
    diaId: Long? = null,
    onBack: () -> Unit,
    viewModel: LocalDiaEditViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditMode = diaId != null
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(officeId, diaId) {
        viewModel.initialize(officeId, diaId)
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is LocalDiaEditEvent.SaveSuccess -> onBack()
                is LocalDiaEditEvent.DeleteSuccess -> onBack()
                is LocalDiaEditEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("근무표 삭제") },
            text = { Text("이 근무표를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete()
                    showDeleteDialog = false
                }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "근무표 편집" else "근무표 추가") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "삭제",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = !state.isLoading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "저장")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 교번 ID
            OutlinedTextField(
                value = state.diaId,
                onValueChange = viewModel::onDiaIdChange,
                label = { Text("교번 ID *") },
                placeholder = { Text("예: 1, 2, 비") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 근무 유형
            OutlinedTextField(
                value = state.typeName,
                onValueChange = viewModel::onTypeNameChange,
                label = { Text("근무 유형") },
                placeholder = { Text("예: 평평, 평휴, 휴일") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 유형 제안 칩
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val suggestions = listOf(
                    "평일", "평평", "휴일", "평휴", "휴평", "휴휴", "평토", "토", "토휴", "휴토"
                )
                suggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { viewModel.onTypeNameChange(suggestion) },
                        label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 근무 시간
            OutlinedTextField(
                value = state.workTime,
                onValueChange = viewModel::onWorkTimeChange,
                label = { Text("출근 시간") },
                placeholder = { Text("예: 06:00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 출근 시간
            OutlinedTextField(
                value = state.firstTime,
                onValueChange = viewModel::onFirstTimeChange,
                label = { Text("전반") },
                placeholder = { Text("예: 05:30-06:00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 퇴근 시간
            OutlinedTextField(
                value = state.secondTime,
                onValueChange = viewModel::onSecondTimeChange,
                label = { Text("후반") },
                placeholder = { Text("예: 14:00-16:00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 세번째 시간
            OutlinedTextField(
                value = state.thirdTime,
                onValueChange = viewModel::onThirdTimeChange,
                label = { Text("추가 사업") },
                placeholder = { Text("OPTION") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 전반 횟수
            OutlinedTextField(
                value = state.numTr1,
                onValueChange = viewModel::onNumTr1Change,
                label = { Text("전반 열번") },
                placeholder = { Text("예: 2222") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 후반 횟수
            OutlinedTextField(
                value = state.numTr2,
                onValueChange = viewModel::onNumTr2Change,
                label = { Text("후반 열번") },
                placeholder = { Text("예: 3333") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 총 시간
            OutlinedTextField(
                value = state.totalTime,
                onValueChange = viewModel::onTotalTimeChange,
                label = { Text("총 시간") },
                placeholder = { Text("예: 08:30") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
