package com.sonbum.diacalendar2.domain.repository

import android.net.Uri
import com.sonbum.diacalendar2.domain.model.AppBackupData

interface BackupRepository {
    /**
     * 모든 앱 데이터를 백업 데이터로 수집
     */
    suspend fun collectBackupData(): AppBackupData

    /**
     * 백업 데이터를 파일로 내보내기
     * @param uri 저장할 파일 URI
     * @return 성공 여부
     */
    suspend fun exportToFile(uri: Uri): Result<Unit>

    /**
     * 파일에서 백업 데이터 읽기
     * @param uri 백업 파일 URI
     * @return 백업 데이터
     */
    suspend fun readFromFile(uri: Uri): Result<AppBackupData>

    /**
     * 백업 데이터를 앱에 복원
     * @param backupData 복원할 백업 데이터
     * @param clearExisting 기존 데이터 삭제 여부
     * @return 복원된 항목 수
     */
    suspend fun restoreFromBackup(backupData: AppBackupData, clearExisting: Boolean = true): Result<Int>
}
