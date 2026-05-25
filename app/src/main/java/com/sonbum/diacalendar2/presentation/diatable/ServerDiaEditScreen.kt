package com.sonbum.diacalendar2.presentation.diatable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServerDiaEditScreen(
    diaId: Long,
    onBack: () -> Unit,
    viewModel: ServerDiaEditViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(diaId) {
        viewModel.initialize(diaId)
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is ServerDiaEditEvent.SaveSuccess -> onBack()
                is ServerDiaEditEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("근무표 편집") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
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
            // 서버 데이터 편집 안내 배너
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.shapes.small
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "서버 데이터를 로컬에서 편집합니다.\n서버 동기화 시 초기화될 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 교번 ID
            OutlinedTextField(
                value = state.diaId,
                onValueChange = viewModel::onDiaIdChange,
                label = { Text("교번 ID") },
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

            // 출근 시간
            OutlinedTextField(
                value = state.workTime,
                onValueChange = viewModel::onWorkTimeChange,
                label = { Text("출근 시간") },
                placeholder = { Text("예: 06:00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 전반
            OutlinedTextField(
                value = state.firstTime,
                onValueChange = viewModel::onFirstTimeChange,
                label = { Text("전반") },
                placeholder = { Text("예: 05:30-06:00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 후반
            OutlinedTextField(
                value = state.secondTime,
                onValueChange = viewModel::onSecondTimeChange,
                label = { Text("후반") },
                placeholder = { Text("예: 14:00-16:00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 추가 사업
            OutlinedTextField(
                value = state.thirdTime,
                onValueChange = viewModel::onThirdTimeChange,
                label = { Text("추가 사업") },
                placeholder = { Text("OPTION") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 전반 열번
            OutlinedTextField(
                value = state.numTr1,
                onValueChange = viewModel::onNumTr1Change,
                label = { Text("전반 열번") },
                placeholder = { Text("예: 2222") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 후반 열번
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
