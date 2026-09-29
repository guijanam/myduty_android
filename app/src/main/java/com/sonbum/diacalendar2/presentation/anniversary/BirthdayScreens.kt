package com.sonbum.diacalendar2.presentation.anniversary

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.sonbum.diacalendar2.data.local.datastore.BirthdayDefaults
import com.sonbum.diacalendar2.domain.model.AgeDisplayMode
import com.sonbum.diacalendar2.domain.model.CalendarType
import com.sonbum.diacalendar2.domain.model.BirthdayGroup
import com.sonbum.diacalendar2.domain.model.BirthdayMilestone
import com.sonbum.diacalendar2.domain.model.BirthdayPerson
import com.sonbum.diacalendar2.domain.model.Feb29Policy
import com.sonbum.diacalendar2.domain.model.LeapMonthPolicy
import com.sonbum.diacalendar2.domain.model.MilestoneRuleType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnniversaryScreen(
    onBack: () -> Unit,
    onNavigateToPerson: (Long) -> Unit = {},
    onNavigateToEditPerson: (Long?) -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (selectedTab == 0) "생일·나이" else "기념일 관리") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    },
                    actions = {
                        if (selectedTab == 0) {
                            IconButton(onClick = onNavigateToGroups) {
                                Icon(Icons.Default.Groups, contentDescription = "그룹 관리")
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "생일 설정")
                            }
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("생일·나이") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("기념일") })
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { onNavigateToEditPerson(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "인물 추가")
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (selectedTab == 0) {
                BirthdayDashboardScreen(onNavigateToPerson)
            } else {
                LegacyAnniversaryScreen(onBack = onBack, embedded = true)
            }
        }
    }
}

@Composable
private fun BirthdayDashboardScreen(
    onNavigateToPerson: (Long) -> Unit,
    viewModel: BirthdayViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    val filteredPeople = remember(state.people, selectedGroupId) {
        if (selectedGroupId == null) state.people else state.people.filter { selectedGroupId in it.groupIds }
    }
    val allowedIds = filteredPeople.map { it.id }.toSet()

    if (!state.isLoading && state.people.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Cake, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("생년월일을 등록해 보세요", style = MaterialTheme.typography.titleMedium)
                Text("나이와 다음 생일을 자동으로 계산합니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedGroupId == null, onClick = { selectedGroupId = null }, label = { Text("전체") })
                state.groups.forEach { group ->
                    FilterChip(
                        selected = selectedGroupId == group.id,
                        onClick = { selectedGroupId = group.id },
                        label = { Text(group.name) }
                    )
                }
            }
        }
        if (state.today.any { it.person.id in allowedIds }) {
            item { SectionTitle("오늘 생일", "🎂") }
            items(state.today.filter { it.person.id in allowedIds }, key = { "today-${it.person.id}" }) {
                BirthdayPersonCard(it, onNavigateToPerson)
            }
        }
        item { SectionTitle("다가오는 생일", "📅") }
        val upcoming = state.upcoming.filter { it.person.id in allowedIds }
        if (upcoming.isEmpty()) item { EmptySection("향후 1년 내 생일이 없습니다") }
        else items(upcoming, key = { "upcoming-${it.person.id}" }) { BirthdayPersonCard(it, onNavigateToPerson) }

        item { SectionTitle("이번 달 생일자", "🗓️") }
        val month = state.thisMonth.filter { it.person.id in allowedIds }
        if (month.isEmpty()) item { EmptySection("이번 달 생일자가 없습니다") }
        else items(month, key = { "month-${it.person.id}" }) { BirthdayPersonCard(it, onNavigateToPerson) }

        item { SectionTitle("주요 기념 나이", "✨") }
        val milestones = state.milestones.filter { it.personId in allowedIds }.take(12)
        if (milestones.isEmpty()) item { EmptySection("향후 5년 내 주요 기념 나이가 없습니다") }
        else items(milestones, key = { "milestone-${it.milestoneId}" }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${item.personName} · ${item.milestoneName}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(item.date.format(DateTimeFormatter.ofPattern("yyyy.M.d")), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun SectionTitle(title: String, emoji: String) {
    Text("$emoji $title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptySection(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun BirthdayPersonCard(card: BirthdayCardUi, onClick: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(card.person.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileImage(card.person.photoPath, card.person.name)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(card.person.name)
                        if (card.person.relationship.isNotBlank()) append(" · ${card.person.relationship}")
                    },
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${card.ageText} · ${if (card.occurrence.isLunar) "음력 → " else ""}${card.occurrence.date.monthValue}월 ${card.occurrence.date.dayOfMonth}일",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                when {
                    card.daysUntil == 0L -> "오늘"
                    card.daysUntil > 0 -> "D-${card.daysUntil}"
                    else -> "D+${-card.daysUntil}"
                },
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProfileImage(path: String?, name: String, size: Int = 52) {
    if (!path.isNullOrBlank() && File(path).exists()) {
        AsyncImage(
            model = File(path), contentDescription = "$name 프로필 사진",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(CircleShape)
        )
    } else {
        Box(
            Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayPersonDetailScreen(
    personId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: BirthdayViewModel = koinViewModel()
) {
    var person by remember { mutableStateOf<BirthdayPerson?>(null) }
    val milestones by viewModel.observeMilestones(personId).collectAsState(initial = emptyList())
    var showDelete by remember { mutableStateOf(false) }
    var showMilestone by remember { mutableStateOf(false) }
    var editingMilestone by remember { mutableStateOf<BirthdayMilestone?>(null) }
    LaunchedEffect(personId) { person = viewModel.getPerson(personId) }
    LaunchedEffect(Unit) {
        viewModel.event.collect { if (it is BirthdayEvent.Saved) onBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person?.name ?: "인물 상세") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") } },
                actions = {
                    IconButton(onClick = { onEdit(personId) }) { Icon(Icons.Default.Edit, "수정") }
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.Delete, "삭제") }
                }
            )
        }
    ) { padding ->
        val value = person
        if (value == null) return@Scaffold
        val age = viewModel.ageSnapshot(value)
        val next = viewModel.nextBirthday(value)
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileImage(value.photoPath, value.name, 78)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(value.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(value.relationship.ifBlank { "관계 미설정" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${viewModel.zodiac(value).orEmpty()} · ${viewModel.westernZodiac(value).orEmpty()}")
                    }
                }
            }
            item {
                InfoCard("생년월일") {
                    Text("${value.birthYear}년 ${value.birthMonth}월 ${value.birthDay}일 (${if (value.calendarType == CalendarType.LUNAR) if (value.isLeapMonth) "음력 윤달" else "음력" else "양력"})")
                    viewModel.solarBirthDate(value)?.let { Text("양력 기준 $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("시간대 ${value.timeZoneId}", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                InfoCard("현재 나이") {
                    if (age != null) {
                        Text("만 ${age.fullAge}세 · 세는나이 ${age.countingAge}세 · 연 나이 ${age.yearAge}세")
                        Text("생후 ${age.monthsSinceBirth}개월 · 태어난 지 ${age.daysSinceBirth}일")
                        Text("올해 만 ${age.ageTurningThisYear}세가 됩니다")
                    }
                }
            }
            item {
                InfoCard("다음 생일") {
                    Text(next?.toString() ?: "계산 가능한 다음 생일이 없습니다")
                    Text(if (value.calendarType == CalendarType.LUNAR) "매년 음력을 양력으로 변환합니다" else "양력 생일", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("주요 기념 나이", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { editingMilestone = null; showMilestone = true }) { Text("추가") }
                }
            }
            items(milestones, key = { it.id }) { milestone ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = milestone.enabled,
                            onCheckedChange = { viewModel.saveMilestone(milestone.copy(enabled = it)) }
                        )
                        Text("${milestone.name} · ${milestoneRuleText(milestone)}", Modifier.weight(1f))
                        IconButton(onClick = { editingMilestone = milestone; showMilestone = true }) {
                            Icon(Icons.Default.Edit, "수정")
                        }
                        if (!milestone.isDefault) {
                            IconButton(onClick = { viewModel.deleteMilestone(milestone) }) { Icon(Icons.Default.Delete, "삭제") }
                        }
                    }
                }
            }
            item {
                InfoCard("연동") {
                    Text(if (value.notificationEnabled) "생일 알림 사용" else "생일 알림 끔")
                    if (value.notificationEnabled) {
                        val offsets = value.notificationOffsets.sortedDescending().joinToString("·") {
                            if (it == 0) "당일" else "${it}일 전"
                        }
                        Text("$offsets · 현지 %02d:%02d".format(value.notificationHour, value.notificationMinute), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (value.calendarSyncEnabled) "기기 캘린더 등록" else "기기 캘린더 등록 안 함")
                }
            }
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("인물 삭제") }, text = { Text("${person?.name}의 생일 정보를 삭제할까요?") },
            confirmButton = { TextButton(onClick = { showDelete = false; viewModel.deletePerson(personId) }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("취소") } }
        )
    }
    if (showMilestone) {
        MilestoneEditDialog(personId = personId, initial = editingMilestone, onDismiss = { showMilestone = false }) {
            viewModel.saveMilestone(it); showMilestone = false
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayPersonEditScreen(
    personId: Long?,
    onBack: () -> Unit,
    viewModel: BirthdayViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val defaults by viewModel.defaults.collectAsStateWithLifecycle()
    var original by remember { mutableStateOf<BirthdayPerson?>(null) }
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var year by remember { mutableStateOf(LocalDate.now().year.toString()) }
    var month by remember { mutableStateOf("1") }
    var day by remember { mutableStateOf("1") }
    var calendarType by remember { mutableStateOf(CalendarType.SOLAR) }
    var leapMonth by remember { mutableStateOf(false) }
    var timeZoneId by remember { mutableStateOf(ZoneId.systemDefault().id) }
    var ageMode by remember { mutableStateOf(defaults.ageDisplayMode) }
    var leapPolicy by remember { mutableStateOf(LeapMonthPolicy.REGULAR_SAME_MONTH) }
    var feb29Policy by remember { mutableStateOf(Feb29Policy.FEBRUARY_28) }
    var notifications by remember { mutableStateOf(true) }
    var offsets by remember { mutableStateOf(defaults.notificationOffsets) }
    var notificationHour by remember { mutableStateOf(defaults.notificationHour.toString()) }
    var notificationMinute by remember { mutableStateOf(defaults.notificationMinute.toString()) }
    var calendarSync by remember { mutableStateOf(true) }
    var groupIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photoUri = it }

    LaunchedEffect(personId) {
        val p = personId?.let { viewModel.getPerson(it) }
        if (p != null) {
            original = p; name = p.name; relationship = p.relationship
            year = p.birthYear.toString(); month = p.birthMonth.toString(); day = p.birthDay.toString()
            calendarType = p.calendarType; leapMonth = p.isLeapMonth; timeZoneId = p.timeZoneId
            ageMode = p.ageDisplayMode; leapPolicy = p.leapMonthPolicy; feb29Policy = p.feb29Policy
            notifications = p.notificationEnabled; offsets = p.notificationOffsets
            notificationHour = p.notificationHour.toString(); notificationMinute = p.notificationMinute.toString()
            calendarSync = p.calendarSyncEnabled; groupIds = p.groupIds
        }
    }
    LaunchedEffect(Unit) {
        viewModel.event.collect { event -> if (event is BirthdayEvent.Saved) onBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (personId == null) "인물 추가" else "인물 수정") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") } },
                actions = {
                    TextButton(
                        enabled = name.isNotBlank() && validDateInput(year, month, day, calendarType) &&
                            isValidZoneId(timeZoneId) && notificationHour.toIntOrNull() in 0..23 &&
                            notificationMinute.toIntOrNull() in 0..59,
                        onClick = {
                            val base = original
                            viewModel.savePerson(
                                BirthdayPerson(
                                    id = base?.id ?: 0, name = name.trim(), photoPath = base?.photoPath,
                                    relationship = relationship.trim(), birthYear = year.toInt(), birthMonth = month.toInt(), birthDay = day.toInt(),
                                    calendarType = calendarType, isLeapMonth = leapMonth, timeZoneId = timeZoneId,
                                    ageDisplayMode = ageMode, leapMonthPolicy = leapPolicy, feb29Policy = feb29Policy,
                                    notificationEnabled = notifications, notificationOffsets = offsets,
                                    notificationHour = notificationHour.toInt(),
                                    notificationMinute = notificationMinute.toInt(),
                                    calendarSyncEnabled = calendarSync, groupIds = groupIds,
                                    createdAt = base?.createdAt ?: System.currentTimeMillis()
                                ),
                                photoUri
                            )
                        }
                    ) { Text("저장") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (photoUri != null) {
                        AsyncImage(photoUri, "선택한 프로필 사진", Modifier.size(88.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    } else ProfileImage(original?.photoPath, name.ifBlank { "인물" }, 88)
                    TextButton(onClick = { photoPicker.launch("image/*") }) { Text("사진 선택") }
                }
            }
            item { OutlinedTextField(name, { name = it }, label = { Text("이름") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item { OutlinedTextField(relationship, { relationship = it }, label = { Text("관계") }, placeholder = { Text("예: 어머니, 친구, 반려동물") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                Text("생년월일", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, Modifier.weight(1.4f), label = { Text("연도") })
                    OutlinedTextField(month, { month = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), label = { Text("월") })
                    OutlinedTextField(day, { day = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), label = { Text("일") })
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalendarType.entries.forEach { type ->
                        FilterChip(selected = calendarType == type, onClick = { calendarType = type }, label = { Text(if (type == CalendarType.SOLAR) "양력" else "음력") })
                    }
                    if (calendarType == CalendarType.LUNAR) {
                        FilterChip(selected = leapMonth, onClick = { leapMonth = !leapMonth }, label = { Text("윤달") })
                    }
                }
            }
            if (calendarType == CalendarType.LUNAR && leapMonth) item {
                ChoiceRow("윤달 없는 해", LeapMonthPolicy.entries, leapPolicy, { leapPolicy = it }) {
                    if (it == LeapMonthPolicy.REGULAR_SAME_MONTH) "평달 기념" else "해당 연도 건너뜀"
                }
            }
            if (calendarType == CalendarType.SOLAR && month == "2" && day == "29") item {
                ChoiceRow("2월 29일", Feb29Policy.entries, feb29Policy, { feb29Policy = it }) {
                    when (it) { Feb29Policy.FEBRUARY_28 -> "2월 28일"; Feb29Policy.MARCH_1 -> "3월 1일"; else -> "윤년에만" }
                }
            }
            item { OutlinedTextField(timeZoneId, { timeZoneId = it }, label = { Text("시간대") }, supportingText = { Text("IANA ID, 예: Asia/Seoul") }, modifier = Modifier.fillMaxWidth()) }
            item {
                ChoiceRow("대표 나이", AgeDisplayMode.entries, ageMode, { ageMode = it }, ::ageModeLabel)
            }
            item {
                Text("그룹", fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.groups.forEach { group ->
                        FilterChip(
                            selected = group.id in groupIds,
                            onClick = { groupIds = if (group.id in groupIds) groupIds - group.id else groupIds + group.id },
                            label = { Text(group.name) }
                        )
                    }
                }
            }
            item { ToggleRow("생일 알림", notifications) { notifications = it } }
            if (notifications) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("알림 시점", fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 7, 3, 1, 0).forEach { value ->
                            FilterChip(
                                selected = value in offsets,
                                onClick = { offsets = if (value in offsets) offsets - value else offsets + value },
                                label = { Text(if (value == 0) "당일" else "${value}일 전") }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            notificationHour, { notificationHour = it.filter(Char::isDigit).take(2) },
                            Modifier.weight(1f), label = { Text("현지 시") }, singleLine = true
                        )
                        OutlinedTextField(
                            notificationMinute, { notificationMinute = it.filter(Char::isDigit).take(2) },
                            Modifier.weight(1f), label = { Text("분") }, singleLine = true
                        )
                    }
                }
            }
            item { ToggleRow("기기 캘린더에 등록", calendarSync) { calendarSync = it } }
        }
    }
}

@Composable
private fun <T> ChoiceRow(title: String, values: List<T>, selected: T, onSelected: (T) -> Unit, label: (T) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value -> FilterChip(selected = value == selected, onClick = { onSelected(value) }, label = { Text(label(value)) }) }
        }
    }
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Switch(checked, onChecked)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayGroupManagerScreen(onBack: () -> Unit, viewModel: BirthdayViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<BirthdayGroup?>(null) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("생일 그룹") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { editingGroup = null; showAdd = true }) { Icon(Icons.Default.Add, "그룹 추가") } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            items(state.groups, key = { it.id }) { group ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(group.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (group.isDefault) Text("기본", color = MaterialTheme.colorScheme.primary)
                    else {
                        IconButton(onClick = { editingGroup = group; showAdd = true }) { Icon(Icons.Default.Edit, "수정") }
                        IconButton(onClick = { viewModel.deleteGroup(group) }) { Icon(Icons.Default.Delete, "삭제") }
                    }
                }
                HorizontalDivider()
            }
        }
    }
    if (showAdd) {
        var name by remember(editingGroup) { mutableStateOf(editingGroup?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showAdd = false }, title = { Text(if (editingGroup == null) "그룹 추가" else "그룹 수정") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("그룹 이름") }, singleLine = true) },
            confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
                viewModel.saveGroup(
                    editingGroup?.copy(name = name.trim())
                        ?: BirthdayGroup(name = name.trim(), sortOrder = state.groups.size)
                )
                showAdd = false
            }) { Text(if (editingGroup == null) "추가" else "저장") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("취소") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BirthdaySettingsScreen(onBack: () -> Unit, viewModel: BirthdayViewModel = koinViewModel()) {
    val saved by viewModel.defaults.collectAsStateWithLifecycle()
    val calendars by viewModel.deviceCalendars.collectAsStateWithLifecycle()
    var value by remember(saved) { mutableStateOf(saved) }
    val calendarPermissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    )
    LaunchedEffect(calendarPermissions.allPermissionsGranted) {
        if (calendarPermissions.allPermissionsGranted) viewModel.loadDeviceCalendars()
    }
    LaunchedEffect(calendars, value.calendarSyncEnabled) {
        if (value.calendarSyncEnabled && calendars.isNotEmpty() && calendars.none { it.id == value.calendarId }) {
            value = value.copy(calendarId = calendars.first().id)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("생일 설정") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") } },
                actions = { TextButton(onClick = { viewModel.saveDefaults(value); onBack() }) { Text("저장") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ChoiceRow("새 인물의 대표 나이", AgeDisplayMode.entries, value.ageDisplayMode, { value = value.copy(ageDisplayMode = it) }, ::ageModeLabel) }
            item { ToggleRow("기기 캘린더 동기화", value.calendarSyncEnabled) { value = value.copy(calendarSyncEnabled = it) } }
            if (value.calendarSyncEnabled) {
                if (!calendarPermissions.allPermissionsGranted) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("생일을 기기 캘린더에 등록하려면 캘린더 읽기·쓰기 권한이 필요합니다")
                            Button(onClick = { calendarPermissions.launchMultiplePermissionRequest() }) {
                                Text("캘린더 권한 허용")
                            }
                            Text("권한을 거부해도 앱 안의 생일과 알림은 계속 사용할 수 있습니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    item { Text("동기화할 캘린더", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    items(calendars, key = { it.id }) { calendar ->
                        FilterChip(
                            selected = value.calendarId == calendar.id,
                            onClick = { value = value.copy(calendarId = calendar.id) },
                            label = { Text("${calendar.displayName} · ${calendar.accountName}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
            item {
                Text("기본 알림", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 7, 3, 1, 0).forEach { offset ->
                        FilterChip(
                            selected = offset in value.notificationOffsets,
                            onClick = {
                                val offsets = if (offset in value.notificationOffsets) {
                                    value.notificationOffsets - offset
                                } else value.notificationOffsets + offset
                                value = value.copy(notificationOffsets = offsets)
                            },
                            label = { Text(if (offset == 0) "당일" else "${offset}일 전") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value.notificationHour.toString(),
                        { input -> input.filter(Char::isDigit).take(2).toIntOrNull()?.let { if (it in 0..23) value = value.copy(notificationHour = it) } },
                        Modifier.weight(1f), label = { Text("현지 시") }, singleLine = true
                    )
                    OutlinedTextField(
                        value.notificationMinute.toString(),
                        { input -> input.filter(Char::isDigit).take(2).toIntOrNull()?.let { if (it in 0..59) value = value.copy(notificationMinute = it) } },
                        Modifier.weight(1f), label = { Text("분") }, singleLine = true
                    )
                }
                Text("새로 등록하는 인물에 적용됩니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MilestoneEditDialog(
    personId: Long,
    initial: BirthdayMilestone? = null,
    onDismiss: () -> Unit,
    onSave: (BirthdayMilestone) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var value by remember(initial) { mutableStateOf(initial?.ruleValue?.toString().orEmpty()) }
    var type by remember(initial) { mutableStateOf(initial?.ruleType ?: MilestoneRuleType.FULL_AGE) }
    var notificationEnabled by remember(initial) { mutableStateOf(initial?.notificationEnabled ?: true) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(if (initial == null) "기념 나이 추가" else "기념 나이 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("명칭") })
                ChoiceRow("기준", MilestoneRuleType.entries, type, { type = it }) {
                    when (it) { MilestoneRuleType.DAYS_AFTER_BIRTH -> "출생 후 N일"; MilestoneRuleType.FULL_AGE -> "만 N세"; else -> "세는나이 N세" }
                }
                OutlinedTextField(value, { value = it.filter(Char::isDigit) }, label = { Text("숫자") })
                ToggleRow("이 일정 알림", notificationEnabled) { notificationEnabled = it }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && value.toIntOrNull() != null, onClick = {
                onSave(
                    BirthdayMilestone(
                        id = initial?.id ?: 0,
                        personId = personId,
                        name = name.trim(),
                        ruleType = type,
                        ruleValue = value.toInt(),
                        enabled = initial?.enabled ?: true,
                        notificationEnabled = notificationEnabled,
                        isDefault = initial?.isDefault ?: false
                    )
                )
            }) { Text(if (initial == null) "추가" else "저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

private fun milestoneRuleText(value: BirthdayMilestone): String = when (value.ruleType) {
    MilestoneRuleType.DAYS_AFTER_BIRTH -> "출생 후 ${value.ruleValue}일"
    MilestoneRuleType.FULL_AGE -> "만 ${value.ruleValue}세"
    MilestoneRuleType.COUNTING_AGE_YEAR -> "세는나이 ${value.ruleValue}세"
}

private fun ageModeLabel(mode: AgeDisplayMode): String = when (mode) {
    AgeDisplayMode.FULL_AGE -> "만 나이"
    AgeDisplayMode.COUNTING_AGE -> "세는나이"
    AgeDisplayMode.YEAR_AGE -> "연 나이"
    AgeDisplayMode.MONTHS -> "개월 수"
    AgeDisplayMode.DAYS -> "일수"
}

private fun validDateInput(year: String, month: String, day: String, calendarType: CalendarType): Boolean {
    val y = year.toIntOrNull() ?: return false
    val m = month.toIntOrNull() ?: return false
    val d = day.toIntOrNull() ?: return false
    if (y !in 1000..LocalDate.now().year || m !in 1..12) return false
    return if (calendarType == CalendarType.SOLAR) {
        runCatching { LocalDate.of(y, m, d) }.isSuccess
    } else d in 1..30
}

private fun isValidZoneId(value: String): Boolean = runCatching { ZoneId.of(value) }.isSuccess
