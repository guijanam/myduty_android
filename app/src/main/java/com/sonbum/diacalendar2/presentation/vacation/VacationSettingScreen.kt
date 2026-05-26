package com.sonbum.diacalendar2.presentation.vacation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonbum.diacalendar2.domain.model.VacationType
import org.koin.androidx.compose.koinViewModel

@Composable
fun VacationSettingScreen(
    onBack: () -> Unit,
    viewModel: VacationSettingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VacationSettingContent(
        state = state,
        onBack = onBack,
        onAdd = { name, shortName, quota, resetMonthDay ->
            viewModel.addVacationType(name, shortName, quota, resetMonthDay)
        },
        onDelete = viewModel::deleteVacationType,
        onUpdate = { id, name, shortName, quota, resetMonthDay ->
            viewModel.updateVacationType(id, name, shortName, quota, resetMonthDay)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VacationSettingContent(
    state: VacationSettingState,
    onBack: () -> Unit,
    onAdd: (String, String, Int, String) -> Unit,
    onDelete: (Long) -> Unit,
    onUpdate: (Long, String, String, Int, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingVacation by remember { mutableStateOf<VacationType?>(null) }
    var deletingVacation by remember { mutableStateOf<VacationType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "근태이름(갯수) 설정",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "근태종류 추가"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
	            .padding(bottom = 70.dp)
                .fillMaxSize()
                .padding(paddingValues)
	            //.verticalScroll(rememberScrollState())
        ) {
	        Row(
		        verticalAlignment = Alignment.CenterVertically,
		        modifier = Modifier
			        .fillMaxWidth()
			        .clip(RoundedCornerShape(8.dp))
			        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
			        .padding(12.dp)
	        ) {
		        Icon(
			        imageVector = Icons.Default.Info,
			        contentDescription = null,
			        tint = MaterialTheme.colorScheme.error,
			        modifier = Modifier.size(20.dp)
		        )
		        Spacer(modifier = Modifier.width(8.dp))
		        Text(
			        text = "직접 근태종류를 편집하세요.근태 사용에 반영됩니다.\n근태이름과 [약어],갯수를 설정하세요.",
			        style = MaterialTheme.typography.bodySmall,
			        color = MaterialTheme.colorScheme.onErrorContainer
		        )
	        }
	        Spacer(modifier = Modifier.height(12.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.vacationTypes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "등록된 근태가 없습니다.\n+ 버튼을 눌러 추가해주세요.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        items = state.vacationTypes,
                        key = { it.id }
                    ) { vacationType ->
                        VacationTypeRow(
                            vacationType = vacationType,
                            onDelete = { deletingVacation = vacationType },
                            onEdit = { editingVacation = vacationType }
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        VacationDialog(
            title = "근태종류 추가",
            confirmLabel = "추가",
            initial = null,
            onConfirm = { name, shortName, quota, resetMonthDay ->
                onAdd(name, shortName, quota, resetMonthDay)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingVacation?.let { vacation ->
        VacationDialog(
            title = "근태 편집",
            confirmLabel = "저장",
            initial = vacation,
            onConfirm = { newName, newShortName, quota, resetMonthDay ->
                onUpdate(vacation.id, newName, newShortName, quota, resetMonthDay)
                editingVacation = null
            },
            onDismiss = { editingVacation = null }
        )
    }

    deletingVacation?.let { vacation ->
        AlertDialog(
            onDismissRequest = { deletingVacation = null },
            title = {
                Text(
                    text = "근태 삭제",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("\"${vacation.name}\"을(를) 삭제하시겠습니까?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(vacation.id)
                        deletingVacation = null
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingVacation = null }) {
                    Text("취소")
                }
            }
        )
    }

}

@Composable
private fun VacationTypeRow(
    vacationType: VacationType,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 이름 + 약어 + 갯수 표시
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = vacationType.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[${vacationType.shortName}]",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (vacationType.annualQuota > 0) {
                    Text(
                        text = "${vacationType.annualQuota}일 (${vacationType.resetMonthDay} 초기화)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // 편집 버튼
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "편집",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 삭제 버튼
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun VacationDialog(
    title: String,
    confirmLabel: String,
    initial: VacationType?,
    onConfirm: (String, String, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var shortName by remember { mutableStateOf(initial?.shortName ?: "") }
    var quotaText by remember {
        mutableStateOf(initial?.annualQuota?.takeIf { it > 0 }?.toString() ?: "")
    }
    val initialParts = (initial?.resetMonthDay ?: "01-01").split("-")
    var resetMonth by remember { mutableStateOf(initialParts.getOrNull(0)?.toIntOrNull() ?: 1) }
    var resetDay by remember { mutableStateOf(initialParts.getOrNull(1)?.toIntOrNull() ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("근태 이름") },
                    placeholder = { Text("예: 육아휴직") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    label = { Text("달력 약어") },
                    placeholder = { Text("예: 육휴") },
                    supportingText = { Text("달력에 표시될 짧은 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quotaText,
                    onValueChange = { v -> quotaText = v.filter { it.isDigit() }.take(3) },
                    label = { Text("연간 갯수 (일)") },
                    placeholder = { Text("예: 15") },
                    supportingText = { Text("비워두면 미설정") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "초기화 날짜 (매년)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonthDayDropdown(
                        label = "월",
                        value = resetMonth,
                        range = 1..12,
                        onSelected = { resetMonth = it; if (resetDay > daysInMonth(it)) resetDay = daysInMonth(it) },
                        modifier = Modifier.weight(1f)
                    )
                    MonthDayDropdown(
                        label = "일",
                        value = resetDay,
                        range = 1..daysInMonth(resetMonth),
                        onSelected = { resetDay = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "기본 매년 1월 1일 초기화",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val quota = quotaText.toIntOrNull() ?: 0
                    val resetStr = "%02d-%02d".format(resetMonth, resetDay)
                    onConfirm(name, shortName, quota, resetStr)
                },
                enabled = name.isNotBlank() && shortName.isNotBlank()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

private fun daysInMonth(month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> 29
    else -> 31
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthDayDropdown(
    label: String,
    value: Int,
    range: IntRange,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            range.forEach { v ->
                DropdownMenuItem(
                    text = { Text(v.toString()) },
                    onClick = {
                        onSelected(v)
                        expanded = false
                    }
                )
            }
        }
    }
}
