package com.sonbum.diacalendar2.presentation.localoffice

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.LocalOffice
import com.sonbum.diacalendar2.domain.repository.LocalOfficeRepository
import com.sonbum.diacalendar2.domain.usecase.BackupRestoreUseCase
import com.sonbum.diacalendar2.domain.usecase.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

data class LocalOfficeListState(
    val offices: List<LocalOffice> = emptyList(),
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isDownloading: Boolean = false
)

sealed interface LocalOfficeListEvent {
    data class ExportSuccess(val uri: Uri) : LocalOfficeListEvent
    data class ImportSuccess(val result: ImportResult) : LocalOfficeListEvent
    data class DownloadSuccess(val result: ImportResult) : LocalOfficeListEvent
    data class Error(val message: String) : LocalOfficeListEvent
}

class LocalOfficeListViewModel(
    private val localOfficeRepository: LocalOfficeRepository,
    private val backupRestoreUseCase: BackupRestoreUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LocalOfficeListState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<LocalOfficeListEvent>()
    val event = _event.asSharedFlow()

    init {
        observeOffices()
    }

    private fun observeOffices() {
        viewModelScope.launch {
            localOfficeRepository.getAllLocalOffices().collect { offices ->
                _state.update { it.copy(offices = offices, isLoading = false) }
            }
        }
    }

    fun deleteOffice(id: Long) {
        viewModelScope.launch {
            localOfficeRepository.deleteLocalOffice(id)
        }
    }

    fun exportData(outputStream: OutputStream, uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            try {
                backupRestoreUseCase.exportToStream(outputStream)
                _event.emit(LocalOfficeListEvent.ExportSuccess(uri))
            } catch (e: Exception) {
                _event.emit(LocalOfficeListEvent.Error("내보내기 실패: ${e.message}"))
            } finally {
                _state.update { it.copy(isExporting = false) }
            }
        }
    }

    fun importData(inputStream: InputStream) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true) }
            try {
                val result = backupRestoreUseCase.importFromStream(inputStream)
                _event.emit(LocalOfficeListEvent.ImportSuccess(result))
            } catch (e: Exception) {
                _event.emit(LocalOfficeListEvent.Error("가져오기 실패: ${e.message}"))
            } finally {
                _state.update { it.copy(isImporting = false) }
            }
        }
    }

    fun downloadFromUrl(urlString: String) {
        viewModelScope.launch {
            _state.update { it.copy(isDownloading = true) }
            try {
                val downloadUrl = convertToDirectDownloadUrl(urlString)
                val bytes = withContext(Dispatchers.IO) {
                    downloadWithRedirects(downloadUrl).use { it.readBytes() }
                }
                val result = backupRestoreUseCase.importFromStream(bytes.inputStream())
                _event.emit(LocalOfficeListEvent.DownloadSuccess(result))
            } catch (e: Exception) {
                val errorMsg = e.message ?: e.javaClass.simpleName
                _event.emit(LocalOfficeListEvent.Error("다운로드 실패: $errorMsg"))
            } finally {
                _state.update { it.copy(isDownloading = false) }
            }
        }
    }

    private fun downloadWithRedirects(urlString: String, maxRedirects: Int = 5): InputStream {
        var currentUrl = urlString
        var redirectCount = 0

        while (redirectCount < maxRedirects) {
            val url = URL(currentUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = false // 수동으로 리다이렉트 처리
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode

            when {
                responseCode == HttpURLConnection.HTTP_OK -> {
                    return connection.inputStream
                }
                responseCode in listOf(
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307, 308
                ) -> {
                    val location = connection.getHeaderField("Location")
                        ?: throw Exception("리다이렉트 위치를 찾을 수 없습니다")
                    currentUrl = if (location.startsWith("http")) {
                        location
                    } else {
                        URL(url, location).toString()
                    }
                    connection.disconnect()
                    redirectCount++
                }
                else -> {
                    throw Exception("다운로드 실패 (HTTP $responseCode)")
                }
            }
        }
        throw Exception("리다이렉트가 너무 많습니다")
    }

    private fun convertToDirectDownloadUrl(urlString: String): String {
        // Google Drive 공유 링크를 직접 다운로드 URL로 변환
        // 형식 1: https://drive.google.com/file/d/FILE_ID/view?usp=sharing
        // 형식 2: https://drive.google.com/open?id=FILE_ID
        // 형식 3: https://drive.google.com/uc?id=FILE_ID&export=download

        val fileIdPattern1 = Regex("drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)")
        val fileIdPattern2 = Regex("drive\\.google\\.com/open\\?id=([a-zA-Z0-9_-]+)")
        val fileIdPattern3 = Regex("drive\\.google\\.com/uc\\?id=([a-zA-Z0-9_-]+)")

        val fileId = fileIdPattern1.find(urlString)?.groupValues?.get(1)
            ?: fileIdPattern2.find(urlString)?.groupValues?.get(1)
            ?: fileIdPattern3.find(urlString)?.groupValues?.get(1)

        return if (fileId != null) {
            "https://drive.google.com/uc?export=download&id=$fileId"
        } else {
            // Google Drive 링크가 아니면 원본 URL 사용
            urlString
        }
    }
}
