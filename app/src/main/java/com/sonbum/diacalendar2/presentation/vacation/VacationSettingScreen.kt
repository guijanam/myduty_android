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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonbum.diacalendar2.domain.model.VacationType
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate

@Composable
fun VacationSettingScreen(
    onBack: () -> Unit,
    viewModel: VacationSettingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VacationSettingContent(
        state = state,
        onBack = onBack,
        onAdd = { name, shortName, quota, resetMonthDay, grantDate, expiryDate ->
            viewModel.addVacationType(name, shortName, quota, resetMonthDay, grantDate, expiryDate)
        },
        onDelete = viewModel::deleteVacationType,
        onUpdate = { id, name, shortName, quota, resetMonthDay, grantDate, expiryDate ->
            viewModel.updateVacationType(id, name, shortName, quota, resetMonthDay, grantDate, expiryDate)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VacationSettingContent(
    state: VacationSettingState,
    onBack: () -> Unit,
    onAdd: (String, String, Int, String, String, String) -> Unit,
    onDelete: (Long) -> Unit,
    onUpdate: (Long, String, String, Int, String, String, String) -> Unit
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
            onConfirm = { name, shortName, quota, resetMonthDay, grantDate, expiryDate ->
                onAdd(name, shortName, quota, resetMonthDay, grantDate, expiryDate)
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
            onConfirm = { newName, newShortName, quota, resetMonthDay, grantDate, expiryDate ->
                onUpdate(vacation.id, newName, newShortName, quota, resetMonthDay, grantDate, expiryDate)
                editingVacation = null
            },
            onDismiss = { editingVacation = null }
        )
    }

    deletingVacation?.let { vacation ->
        AlertDialog(
            onDismissRequest = { deletingVacation = null },
            title = { Text(text = "근태 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("\"${vacation.name}\"을(를) 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(vacation.id)
                    deletingVacation = null
                }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingVacation = null }) { Text("취소") }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = vacationType.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[${vacationType.shortName}]",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (vacationType.isMultiYear) {
                    // 다년도 근태: 발생~소멸 날짜 + 총 갯수
                    val grantLabel = formatDateLabel(vacationType.grantDate)
                    val expiryLabel = formatDateLabel(vacationType.expiryDate)
                    Text(
                        text = if (vacationType.annualQuota > 0)
                            "총 ${vacationType.annualQuota}일 · $grantLabel ~ $expiryLabel"
                        else
                            "$grantLabel ~ $expiryLabel",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else if (vacationType.annualQuota > 0) {
                    // 일반 근태: 연간 갯수 + 초기화 날짜
                    Text(
                        text = "${vacationType.annualQuota}일 (${vacationType.resetMonthDay} 초기화)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "편집", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** "YYYY-MM-DD" → "YYYY년 MM월 DD일" 형식으로 변환 */
private fun formatDateLabel(dateStr: String): String {
    val parts = dateStr.split("-")
    if (parts.size < 3) return dateStr
    val y = parts[0].toIntOrNull() ?: return dateStr
    val m = parts[1].toIntOrNull() ?: return dateStr
    val d = parts[2].toIntOrNull() ?: return dateStr
    return "${y}년 ${m}월 ${d}일"
}

// ─── Dialog ────────────────────────────────────────────────────────────────

@Composable
private fun VacationDialog(
    title: String,
    confirmLabel: String,
    initial: VacationType?,
    onConfirm: (String, String, Int, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = LocalDate.now().year

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var shortName by remember { mutableStateOf(initial?.shortName ?: "") }
    var quotaText by remember {
        mutableStateOf(initial?.annualQuota?.takeIf { it > 0 }?.toString() ?: "")
    }

    // ── 초기화 날짜 (일반 근태용) ──
    val resetParts = (initial?.resetMonthDay ?: "01-01").split("-")
    var resetMonth by remember { mutableIntStateOf(resetParts.getOrNull(0)?.toIntOrNull() ?: 1) }
    var resetDay by remember { mutableIntStateOf(resetParts.getOrNull(1)?.toIntOrNull() ?: 1) }

    // ── 발생일 (다년도 근태용) ──
    // 기존 grantDate가 있으면 파싱, 없으면 기본값 현재연도-01-01
    val initGrantParts = if (initial?.grantDate?.isNotEmpty() == true)
        initial.grantDate.split("-") else listOf(currentYear.toString(), "01", "01")
    var grantYear by remember { mutableIntStateOf(initGrantParts.getOrNull(0)?.toIntOrNull() ?: currentYear) }
    var grantMonth by remember { mutableIntStateOf(initGrantParts.getOrNull(1)?.toIntOrNull() ?: 1) }
    var grantDay by remember { mutableIntStateOf(initGrantParts.getOrNull(2)?.toIntOrNull() ?: 1) }

    // ── 소멸일 (다년도 근태용) ──
    val initExpiryParts = if (initial?.expiryDate?.isNotEmpty() == true)
        initial.expiryDate.split("-") else listOf((currentYear + 10).toString(), "01", "01")
    var expiryYear by remember { mutableIntStateOf(initExpiryParts.getOrNull(0)?.toIntOrNull() ?: (currentYear + 10)) }
    var expiryMonth by remember { mutableIntStateOf(initExpiryParts.getOrNull(1)?.toIntOrNull() ?: 1) }
    var expiryDay by remember { mutableIntStateOf(initExpiryParts.getOrNull(2)?.toIntOrNull() ?: 1) }

    // ── 다년도 모드 토글 ──
    var isMultiYearMode by remember { mutableStateOf(initial?.isMultiYear == true) }

    // 유효성: 소멸일 >= 발생일
    val grantDateStr = "%04d-%02d-%02d".format(grantYear, grantMonth, grantDay)
    val expiryDateStr = "%04d-%02d-%02d".format(expiryYear, expiryMonth, expiryDay)
    val dateError = isMultiYearMode && expiryDateStr < grantDateStr

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── 기본 정보 ──
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
                    label = { Text("총 갯수 (일)") },
                    placeholder = { Text("예: 15") },
                    supportingText = { Text("비워두면 미설정") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                // ── 다년도 모드 선택 ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "발생/소멸일 설정",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row {
                        TextButton(
                            onClick = { isMultiYearMode = false },
                        ) {
                            Text(
                                text = "매년 초기화",
                                color = if (!isMultiYearMode) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                fontWeight = if (!isMultiYearMode) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        TextButton(
                            onClick = { isMultiYearMode = true },
                        ) {
                            Text(
                                text = "기간설정",
                                color = if (isMultiYearMode) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.outline,
                                fontWeight = if (isMultiYearMode) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                if (isMultiYearMode) {
                    // ── 발생일 년/월/일 ──
                    Text(
                        text = "발생일",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        YearDropdown(
                            label = "년",
                            value = grantYear,
                            range = (currentYear - 10)..(currentYear + 30),
                            onSelected = {
                                grantYear = it
                                if (grantDay > daysInMonth(grantMonth, grantYear)) grantDay = daysInMonth(grantMonth, grantYear)
                            },
                            modifier = Modifier.weight(2f)
                        )
                        MonthDayDropdown(
                            label = "월",
                            value = grantMonth,
                            range = 1..12,
                            onSelected = {
                                grantMonth = it
                                if (grantDay > daysInMonth(it, grantYear)) grantDay = daysInMonth(it, grantYear)
                            },
                            modifier = Modifier.weight(1.5f)
                        )
                        MonthDayDropdown(
                            label = "일",
                            value = grantDay,
                            range = 1..daysInMonth(grantMonth, grantYear),
                            onSelected = { grantDay = it },
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    // ── 소멸일 년/월/일 ──
                    Text(
                        text = "소멸일",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        YearDropdown(
                            label = "년",
                            value = expiryYear,
                            range = (currentYear - 10)..(currentYear + 30),
                            onSelected = {
                                expiryYear = it
                                if (expiryDay > daysInMonth(expiryMonth, expiryYear)) expiryDay = daysInMonth(expiryMonth, expiryYear)
                            },
                            modifier = Modifier.weight(2f)
                        )
                        MonthDayDropdown(
                            label = "월",
                            value = expiryMonth,
                            range = 1..12,
                            onSelected = {
                                expiryMonth = it
                                if (expiryDay > daysInMonth(it, expiryYear)) expiryDay = daysInMonth(it, expiryYear)
                            },
                            modifier = Modifier.weight(1.5f)
                        )
                        MonthDayDropdown(
                            label = "일",
                            value = expiryDay,
                            range = 1..daysInMonth(expiryMonth, expiryYear),
                            onSelected = { expiryDay = it },
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    if (dateError) {
                        Text(
                            text = "소멸일은 발생일 이후여야 합니다",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "다년도 근태: $grantDateStr ~ $expiryDateStr 누적 사용",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                } else {
                    // ── 초기화 날짜 (매년 초기화 모드) ──
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
                            onSelected = {
                                resetMonth = it
                                if (resetDay > daysInMonth(it)) resetDay = daysInMonth(it)
                            },
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val quota = quotaText.toIntOrNull() ?: 0
                    val resetStr = "%02d-%02d".format(resetMonth, resetDay)
                    val grantDate = if (isMultiYearMode) grantDateStr else ""
                    val expiryDate = if (isMultiYearMode) expiryDateStr else ""
                    onConfirm(name, shortName, quota, resetStr, grantDate, expiryDate)
                },
                enabled = name.isNotBlank() && shortName.isNotBlank() && !dateError
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

// ─── Dropdown helpers ──────────────────────────────────────────────────────

private fun daysInMonth(month: Int, year: Int = 2000): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    else -> 31
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearDropdown(
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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            range.forEach { y ->
                DropdownMenuItem(
                    text = { Text(y.toString()) },
                    onClick = { onSelected(y); expanded = false }
                )
            }
        }
    }
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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            range.forEach { v ->
                DropdownMenuItem(
                    text = { Text(v.toString()) },
                    onClick = { onSelected(v); expanded = false }
                )
            }
        }
    }
}
