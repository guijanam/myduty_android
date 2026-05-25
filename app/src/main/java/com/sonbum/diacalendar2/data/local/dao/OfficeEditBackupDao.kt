package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.OfficeEditBackupEntity

@Dao
interface OfficeEditBackupDao {

    @Query("SELECT * FROM office_edit_backups")
    suspend fun getAll(): List<OfficeEditBackupEntity>

    @Query("SELECT * FROM office_edit_backups WHERE officeCode = :officeCode")
    suspend fun getByCode(officeCode: Long): OfficeEditBackupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(backup: OfficeEditBackupEntity)

    @Query("DELETE FROM office_edit_backups")
    suspend fun deleteAll()

    @Query("DELETE FROM office_edit_backups WHERE officeCode = :officeCode")
    suspend fun deleteByCode(officeCode: Long)

    @Query("SELECT COUNT(*) FROM office_edit_backups")
    suspend fun getCount(): Int
}
