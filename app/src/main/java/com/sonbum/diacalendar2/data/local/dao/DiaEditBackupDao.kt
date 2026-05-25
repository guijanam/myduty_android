package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.DiaEditBackupEntity

@Dao
interface DiaEditBackupDao {

    @Query("SELECT * FROM dia_edit_backups")
    suspend fun getAll(): List<DiaEditBackupEntity>

    @Query("SELECT * FROM dia_edit_backups WHERE officeId = :officeId")
    suspend fun getByOfficeId(officeId: Int): List<DiaEditBackupEntity>

    @Query("SELECT * FROM dia_edit_backups WHERE id = :id")
    suspend fun getById(id: Long): DiaEditBackupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(backups: List<DiaEditBackupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(backup: DiaEditBackupEntity)

    @Query("DELETE FROM dia_edit_backups")
    suspend fun deleteAll()

    @Query("DELETE FROM dia_edit_backups WHERE officeId = :officeId")
    suspend fun deleteByOfficeId(officeId: Int)

    @Query("SELECT COUNT(*) FROM dia_edit_backups")
    suspend fun getCount(): Int
}
