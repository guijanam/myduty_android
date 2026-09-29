package com.sonbum.diacalendar2.presentation.menu

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.datastore.MenuPreferences
import com.sonbum.diacalendar2.domain.model.CafeteriaMenu
import com.sonbum.diacalendar2.domain.repository.MenuRepository
import com.sonbum.diacalendar2.ui.theme.DiaCalendar2Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class MenuState(
    val isLoading: Boolean = true,
    val allMenus: List<CafeteriaMenu> = emptyList(),
    val cafeteriaNames: List<String> = emptyList(),
    val selectedCafeteria: String? = null,
    val error: String? = null,
    val dateString: String = ""
) {
    val selectedMenu: CafeteriaMenu?
        get() = allMenus.find { it.cafeteriaName == selectedCafeteria }
}

class MenuViewModel(
    private val menuRepository: MenuRepository,
    private val menuPreferences: MenuPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(MenuState())
    val state: StateFlow<MenuState> = _state.asStateFlow()

    fun loadMenu(dateString: String) {
        _state.value = _state.value.copy(isLoading = true, error = null, dateString = dateString)
        viewModelScope.launch {
            val savedCafeteria = menuPreferences.selectedCafeteria.first()
            val result = menuRepository.getMenusForDate(dateString)

            result.fold(
                onSuccess = { menus ->
                    val names = menus.map { it.cafeteriaName }.distinct()
                    val selected = savedCafeteria
                        ?.takeIf { it in names }
                        ?: names.firstOrNull()

                    _state.value = _state.value.copy(
                        isLoading = false,
                        allMenus = menus,
                        cafeteriaNames = names,
                        selectedCafeteria = selected
                    )
                },
                onFailure = { exception ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = exception.message ?: "메뉴를 불러오지 못했습니다"
                    )
                }
            )
        }
    }

    fun selectCafeteria(name: String) {
        _state.value = _state.value.copy(selectedCafeteria = name)
        viewModelScope.launch {
            menuPreferences.setSelectedCafeteria(name)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    dateString: String,
    onBack: () -> Unit,
    viewModel: MenuViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(dateString) {
        viewModel.loadMenu(dateString)
    }

    MenuScreenContent(
        state = state,
        formattedDate = formatMenuDate(dateString),
        onBack = onBack,
        onUploadClick = {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://cafeteria-nine-psi.vercel.app/analyze")
            )
            context.startActivity(intent)
        },
        onRetry = { viewModel.loadMenu(dateString) },
        onCafeteriaSelected = viewModel::selectCafeteria
    )
}

internal fun formatMenuDate(dateString: String): String = try {
    val date = LocalDate.parse(dateString)
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
    "${date.monthValue}월 ${date.dayOfMonth}일 $dayOfWeek"
} catch (_: Exception) {
    dateString
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MenuScreenContent(
    state: MenuState,
    formattedDate: String,
    onBack: () -> Unit,
    onUploadClick: () -> Unit,
    onRetry: () -> Unit,
    onCafeteriaSelected: (String) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val screenColor = if (isDark) Color(0xFF121419) else Color(0xFFF8FAFD)
    val topBarColor = if (isDark) Color(0xFF121419) else Color(0xFFFDFEFF)

    Scaffold(
        modifier = Modifier.testTag("menu_screen"),
        containerColor = screenColor,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "식단",
                            fontSize = 27.sp,
                            lineHeight = 31.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formattedDate,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onUploadClick,
                        modifier = Modifier.testTag("menu_upload")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "식단메뉴사진업로드"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(message = state.error, onRetry = onRetry)
                state.allMenus.isEmpty() -> EmptyState()
                else -> MenuLoadedContent(
                    state = state,
                    onCafeteriaSelected = onCafeteriaSelected
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "식단을 불러오는 중입니다",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("다시 시도")
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "등록된 식단이 없습니다",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MenuLoadedContent(
    state: MenuState,
    onCafeteriaSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CafeteriaDropdown(
                names = state.cafeteriaNames,
                selected = state.selectedCafeteria.orEmpty(),
                onSelect = onCafeteriaSelected
            )
        }

        state.selectedMenu?.let { menu ->
            item {
                CafeteriaMenuCard(menu = menu)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CafeteriaDropdown(
    names: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) Color(0xFF1C2028) else Color.White
    val borderColor = if (isDark) Color(0xFF353B47) else Color(0xFFD9DEE8)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("cafeteria_selector"),
            shape = RoundedCornerShape(22.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = if (isDark) 0.dp else 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "식당 선택",
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selected,
                        fontSize = 18.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "식당 목록 닫기" else "식당 목록 열기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(30.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = surfaceColor
        ) {
            names.forEach { name ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = name,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelect(name)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

private enum class MealKind(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTag: String
) {
    BREAKFAST(
        title = "조식",
        subtitle = "든든한 하루의 시작",
        icon = Icons.Default.WbSunny,
        testTag = "meal_card_breakfast"
    ),
    LUNCH(
        title = "중식",
        subtitle = "활기찬 오후를 위해",
        icon = Icons.Default.Restaurant,
        testTag = "meal_card_lunch"
    ),
    DINNER(
        title = "석식",
        subtitle = "오늘도 수고하셨습니다",
        icon = Icons.Default.Bedtime,
        testTag = "meal_card_dinner"
    )
}

private data class MealPalette(
    val container: Color,
    val border: Color,
    val accent: Color,
    val iconBackground: Color,
    val itemText: Color,
    val subtitleText: Color
)

@Composable
private fun mealPalette(kind: MealKind): MealPalette {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (kind) {
        MealKind.BREAKFAST -> if (isDark) {
            MealPalette(
                container = Color(0xFF302219),
                border = Color(0xFF503321),
                accent = Color(0xFFFF8750),
                iconBackground = Color(0xFF472C1D),
                itemText = Color(0xFFFFF8F3),
                subtitleText = Color(0xFFD7C2B5)
            )
        } else {
            MealPalette(
                container = Color(0xFFFFF7EE),
                border = Color(0xFFF5E4D2),
                accent = Color(0xFFEF5B19),
                iconBackground = Color(0xFFFFEEDF),
                itemText = Color(0xFF202124),
                subtitleText = Color(0xFF7D7470)
            )
        }

        MealKind.LUNCH -> if (isDark) {
            MealPalette(
                container = Color(0xFF192A20),
                border = Color(0xFF294632),
                accent = Color(0xFF69C982),
                iconBackground = Color(0xFF25402E),
                itemText = Color(0xFFF3FBF5),
                subtitleText = Color(0xFFBBD0C0)
            )
        } else {
            MealPalette(
                container = Color(0xFFF1FAF3),
                border = Color(0xFFD9EDDD),
                accent = Color(0xFF278B43),
                iconBackground = Color(0xFFDFF2E4),
                itemText = Color(0xFF202124),
                subtitleText = Color(0xFF6F7D73)
            )
        }

        MealKind.DINNER -> if (isDark) {
            MealPalette(
                container = Color(0xFF182638),
                border = Color(0xFF294563),
                accent = Color(0xFF73ACF5),
                iconBackground = Color(0xFF223C59),
                itemText = Color(0xFFF3F7FD),
                subtitleText = Color(0xFFB8C9DF)
            )
        } else {
            MealPalette(
                container = Color(0xFFF1F7FF),
                border = Color(0xFFD8E8FB),
                accent = Color(0xFF256CC1),
                iconBackground = Color(0xFFDDEBFC),
                itemText = Color(0xFF202124),
                subtitleText = Color(0xFF6D7888)
            )
        }
    }
}

@Composable
fun CafeteriaMenuCard(menu: CafeteriaMenu) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (menu.breakfast.isNotEmpty()) {
            MealCard(kind = MealKind.BREAKFAST, items = menu.breakfast)
        }
        if (menu.lunch.isNotEmpty()) {
            MealCard(kind = MealKind.LUNCH, items = menu.lunch)
        }
        if (menu.dinner.isNotEmpty()) {
            MealCard(kind = MealKind.DINNER, items = menu.dinner)
        }
    }
}

@Composable
private fun MealCard(
    kind: MealKind,
    items: List<String>
) {
    val palette = mealPalette(kind)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(kind.testTag),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = palette.container),
        border = BorderStroke(1.dp, palette.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = palette.iconBackground,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = kind.icon,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = kind.title,
                        fontSize = 25.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = kind.subtitle,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = palette.subtitleText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontSize = 21.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item,
                            modifier = Modifier.weight(1f),
                            fontSize = 19.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.itemText
                        )
                    }
                }
            }
        }
    }
}

private val previewMenu = CafeteriaMenu(
    cafeteriaName = "본우리집밥 서울교통공사 동대문별관점",
    date = "2026-09-07",
    dayOfWeek = "월요일",
    breakfast = listOf("소고기육개장", "백미밥", "너비아니구이", "호박나물", "오징어젓무침", "포기김치"),
    lunch = listOf("카레라이스 & 구운야채", "백미밥", "돈까스우동국물", "치킨까스 & 칠리소스", "푸실리파스타", "포기김치"),
    dinner = listOf("낙지제육비빔밥", "백미밥", "순두부백탕", "삼색당평채", "그린빈스버섯볶음", "포기김치")
)

@Preview(name = "Menu light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MenuScreenLightPreview() {
    DiaCalendar2Theme(darkTheme = false, dynamicColor = false) {
        MenuScreenContent(
            state = MenuState(
                isLoading = false,
                allMenus = listOf(previewMenu),
                cafeteriaNames = listOf(previewMenu.cafeteriaName),
                selectedCafeteria = previewMenu.cafeteriaName,
                dateString = previewMenu.date
            ),
            formattedDate = "9월 7일 월요일",
            onBack = {},
            onUploadClick = {},
            onRetry = {},
            onCafeteriaSelected = {}
        )
    }
}

@Preview(name = "Menu dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MenuScreenDarkPreview() {
    DiaCalendar2Theme(darkTheme = true, dynamicColor = false) {
        MenuScreenContent(
            state = MenuState(
                isLoading = false,
                allMenus = listOf(previewMenu),
                cafeteriaNames = listOf(previewMenu.cafeteriaName),
                selectedCafeteria = previewMenu.cafeteriaName,
                dateString = previewMenu.date
            ),
            formattedDate = "9월 7일 월요일",
            onBack = {},
            onUploadClick = {},
            onRetry = {},
            onCafeteriaSelected = {}
        )
    }
}

@Preview(name = "Menu narrow", showBackground = true, widthDp = 280, heightDp = 700, fontScale = 1.3f)
@Composable
private fun MenuScreenNarrowPreview() {
    DiaCalendar2Theme(darkTheme = false, dynamicColor = false) {
        CafeteriaMenuCard(menu = previewMenu)
    }
}
