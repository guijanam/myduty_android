package com.sonbum.diacalendar2.presentation.localoffice

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonbum.diacalendar2.domain.model.LocalOffice
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalOfficeListScreen(
    onBack: () -> Unit,
    onAddOffice: () -> Unit,
    onEditOffice: (Long) -> Unit,
    onManageDias: (Long) -> Unit,
    viewModel: LocalOfficeListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var officeToDelete by remember { mutableStateOf<LocalOffice?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadUrl by remember { mutableStateOf("") }

    // SAF: 내보내기 (파일 생성)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val outputStream = context.contentResolver.openOutputStream(it)
            if (outputStream != null) {
                viewModel.exportData(outputStream, it)
            }
        }
    }

    // SAF: 가져오기 (파일 열기)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            if (inputStream != null) {
                viewModel.importData(inputStream)
            }
        }
    }

    // 이벤트 수집
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is LocalOfficeListEvent.ExportSuccess -> {
                    // 공유 인텐트 실행
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "승무소 데이터 공유"))
                }
                is LocalOfficeListEvent.ImportSuccess -> {
                    val result = event.result
                    val message = buildString {
                        append("${result.importedOfficeCount}개 승무소, ")
                        append("${result.importedDiaCount}개 근무표 가져옴")
                        if (result.renamedOffices.isNotEmpty()) {
                            append("\n이름 변경: ${result.renamedOffices.joinToString(", ")}")
                        }
                    }
                    snackbarHostState.showSnackbar(message)
                }
                is LocalOfficeListEvent.DownloadSuccess -> {
                    val result = event.result
                    val message = buildString {
                        append("다운로드 완료: ")
                        append("${result.importedOfficeCount}개 승무소, ")
                        append("${result.importedDiaCount}개 근무표 가져옴")
                        if (result.renamedOffices.isNotEmpty()) {
                            append("\n이름 변경: ${result.renamedOffices.joinToString(", ")}")
                        }
                    }
                    snackbarHostState.showSnackbar(message)
                }
                is LocalOfficeListEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    officeToDelete?.let { office ->
        AlertDialog(
            onDismissRequest = { officeToDelete = null },
            title = { Text("승무소 삭제") },
            text = { Text("\"${office.officeName}\" 승무소와 관련된 모든 근무표 데이터가 삭제됩니다. 계속하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteOffice(office.id)
                    officeToDelete = null
                }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { officeToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }

    // URL 다운로드 다이얼로그
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isDownloading) {
                    showDownloadDialog = false
                    downloadUrl = ""
                }
            },
            title = { Text("예시 파일 다운로드") },
            text = {
                Column {
                    Text(
                        text = "공개된 Google Drive 링크를 입력하세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "예: https://drive.google.com/file/d/.../view",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        label = { Text("URL") },
                        placeholder = { Text("Google Drive 링크 붙여넣기") },
                        singleLine = true,
                        enabled = !state.isDownloading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.isDownloading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("다운로드 중...")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (downloadUrl.isNotBlank()) {
                            viewModel.downloadFromUrl(downloadUrl.trim())
                            showDownloadDialog = false
                            downloadUrl = ""
                        }
                    },
                    enabled = downloadUrl.isNotBlank() && !state.isDownloading
                ) {
                    Text("다운로드")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDownloadDialog = false
                        downloadUrl = ""
                    },
                    enabled = !state.isDownloading
                ) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("승무소 관리") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "더보기")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("내보내기 (.json)") },
                                onClick = {
                                    showMenu = false
                                    if (state.offices.isEmpty()) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("내보낼 승무소가 없습니다")
                                        }
                                        return@DropdownMenuItem
                                    }
                                    val fileName = "diacalendar_${
                                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                                    }.json"
                                    exportLauncher.launch(fileName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("가져오기 (.json)") },
                                onClick = {
                                    showMenu = false
                                    importLauncher.launch(arrayOf("*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("URL에서 다운로드") },
                                onClick = {
                                    showMenu = false
                                    showDownloadDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddOffice) {
                Icon(Icons.Default.Add, contentDescription = "승무소 추가")
            }
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.offices.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
	                    horizontalAlignment = Alignment.CenterHorizontally
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
			                    text = "직접 교번순서와 근무표를 생성하고" +
					                    "\n우측상단의 버튼으로 내보내기와 가져오기로" +
					                    "\n교번순서와 근무표는 공유가능합니다." +
					                    "\n다이아개정시 수정후에 동료들과 공유해서 사용하세요." +
			                            "\n직접 생성한 파일은 내부용 승무소에서 근무를 생성하세요.",
			                    style = MaterialTheme.typography.bodySmall,
			                    color = MaterialTheme.colorScheme.onErrorContainer
		                    )
	                    }
	                    Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "등록된 승무소가 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "하단의 + 버튼으로 추가하세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.offices,
                        key = { it.id }
                    ) { office ->
                        LocalOfficeCard(
                            office = office,
                            onEdit = { onEditOffice(office.id) },
                            onManageDias = { onManageDias(office.id) },
                            onDelete = { officeToDelete = office }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalOfficeCard(
    office: LocalOffice,
    onEdit: () -> Unit,
    onManageDias: () -> Unit,
    onDelete: () -> Unit
) {
    val positions = buildList {
        if (!office.diaTurns1.isNullOrBlank()) add("기관사")
        if (!office.diaTurns2.isNullOrBlank()) add("차장")
        if (!office.subTurns.isNullOrBlank()) add("4조2교대")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = office.officeName,
                style = MaterialTheme.typography.titleMedium
            )
            if (positions.isNotEmpty()) {
                Text(
                    text = positions.joinToString(" / "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "편집",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onManageDias) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "근무표 관리",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
