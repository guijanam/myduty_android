package com.sonbum.diacalendar2.presentation.menu

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BrunchDining
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.datastore.MenuPreferences
import com.sonbum.diacalendar2.domain.model.CafeteriaMenu
import com.sonbum.diacalendar2.domain.repository.MenuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── State ──

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

// ── ViewModel ──

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
                    val selected = if (savedCafeteria != null && names.contains(savedCafeteria)) {
                        savedCafeteria
                    } else {
                        names.firstOrNull()
                    }

                    _state.value = _state.value.copy(
                        isLoading = false,
                        allMenus = menus,
                        cafeteriaNames = names,
                        selectedCafeteria = selected
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "메뉴를 불러오지 못했습니다"
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

// ── Screen ──

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

    val formattedDate = try {
        val date = LocalDate.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN))
    } catch (_: Exception) {
        dateString
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("식단 - $formattedDate") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cafeteria-nine-psi.vercel.app/analyze"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "식단메뉴사진업로드")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.loadMenu(dateString) }) {
                            Text("다시 시도")
                        }
                    }
                }

                state.allMenus.isEmpty() -> {
                    Text(
                        text = "등록된 식단이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 식당 선택 드롭다운
                        CafeteriaDropdown(
                            names = state.cafeteriaNames,
                            selected = state.selectedCafeteria ?: "",
                            onSelect = { viewModel.selectCafeteria(it) },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )

                        // 선택된 식당의 메뉴 카드
                        val menu = state.selectedMenu
                        if (menu != null) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    CafeteriaMenuCard(menu = menu)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 식당 선택 드롭다운 ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CafeteriaDropdown(
    names: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("식당 선택") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            names.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
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

// ── 카페테리아 메뉴 카드 ──

@Composable
fun CafeteriaMenuCard(menu: CafeteriaMenu) {
	Column(modifier = Modifier.fillMaxWidth()) {


		// 2. 조식 카드 (연한 주황색)
		if (menu.breakfast.isNotEmpty()) {
			MealCard(
				title = "조식",
				icon = Icons.Default.BrunchDining,
				items = menu.breakfast,
				containerColor = Color(0xFFFFF3E0), // 배경: Light Orange
				contentColor = Color(0xFFE65100)    // 글자/아이콘: Dark Orange
			)
		}

		// 3. 중식 카드 (연한 녹색)
		if (menu.lunch.isNotEmpty()) {
			MealCard(
				title = "중식",
				icon = Icons.Default.LunchDining,
				items = menu.lunch,
				containerColor = Color(0xFFE8F5E9), // 배경: Light Green
				contentColor = Color(0xFF2E7D32)    // 글자/아이콘: Dark Green
			)
		}

		// 4. 석식 카드 (연한 파란색)
		if (menu.dinner.isNotEmpty()) {
			MealCard(
				title = "석식",
				icon = Icons.Default.DinnerDining,
				items = menu.dinner,
				containerColor = Color(0xFFE3F2FD), // 배경: Light Blue
				contentColor = Color(0xFF1565C0)    // 글자/아이콘: Dark Blue
			)
		}
	}
}

// ── 개별 식사 카드 (재사용 가능한 컴포넌트) ──

@Composable
private fun MealCard(
	title: String,
	icon: ImageVector,
	items: List<String>,
	containerColor: Color,
	contentColor: Color
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp), // 카드 간의 상하 간격을 위해 vertical padding 추가
		shape = RoundedCornerShape(16.dp),
		colors = CardDefaults.cardColors(
			containerColor = containerColor
		),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(
			modifier = Modifier.padding(20.dp)
		) {
			// 타이틀 및 아이콘 영역
			Row(verticalAlignment = Alignment.CenterVertically) {
				Icon(
					imageVector = icon,
					contentDescription = null,
					tint = contentColor,
					modifier = Modifier.size(28.dp) // 큰 아이콘 유지
				)
				Spacer(modifier = Modifier.width(8.dp))
				Text(
					text = title,
					fontSize = 21.sp, // 큰 타이틀 유지
					fontWeight = FontWeight.Bold,
					color = contentColor
				)
			}

			Spacer(modifier = Modifier.height(12.dp))

			// 메뉴 항목 리스트
			items.forEach { item ->
				Row(modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)) {
					Text(
						text = "•",
						fontSize = 18.sp,
						color = Color(0xFF424242) // 연한 배경에서도 잘 보이도록 어두운 회색 고정
					)
					Spacer(modifier = Modifier.width(8.dp))
					Text(
						text = item,
						fontSize = 20.sp, // 큰 메뉴 글씨 유지
						fontWeight = FontWeight.Medium,
						color = Color(0xFF212121) // 연한 배경에서도 잘 보이도록 진한 색상 고정
					)
				}
			}
		}
	}
}
