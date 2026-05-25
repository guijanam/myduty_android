package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonbum.diacalendar2.data.local.entity.LocalDiaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalDiaDao {

    @Query("SELECT * FROM local_dias WHERE localOfficeId = :officeId")
    fun getDiasByOfficeId(officeId: Long): Flow<List<LocalDiaEntity>>

    @Query("SELECT * FROM local_dias WHERE officeName = :officeName")
    fun getDiasByOfficeName(officeName: String): Flow<List<LocalDiaEntity>>

    @Query("SELECT * FROM local_dias WHERE id = :id")
    suspend fun getDiaById(id: Long): LocalDiaEntity?

    @Query("SELECT * FROM local_dias WHERE diaId = :diaId AND officeName = :officeName AND typeName = :typeName")
    suspend fun getDiaByDiaIdAndOfficeAndType(diaId: String, officeName: String, typeName: String): LocalDiaEntity?

    @Query("SELECT * FROM local_dias WHERE diaId = :diaId AND officeName = :officeName LIMIT 1")
    suspend fun getDiaByDiaIdAndOffice(diaId: String, officeName: String): LocalDiaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dia: LocalDiaEntity): Long

    @Update
    suspend fun update(dia: LocalDiaEntity)

    @Query("DELETE FROM local_dias WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM local_dias WHERE localOfficeId = :officeId")
    suspend fun deleteByOfficeId(officeId: Long)

    @Query("SELECT COUNT(*) FROM local_dias WHERE localOfficeId = :officeId")
    suspend fun getCountByOfficeId(officeId: Long): Int

    @Query("SELECT * FROM local_dias")
    fun getAllLocalDias(): Flow<List<LocalDiaEntity>>

    @Query("DELETE FROM local_dias")
    suspend fun deleteAll()
}
