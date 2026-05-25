package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonbum.diacalendar2.data.local.entity.DiaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaDao {

    @Query("SELECT * FROM dias")
    fun getAllDias(): Flow<List<DiaEntity>>

    @Query("SELECT * FROM dias WHERE officeId = :officeId")
    fun getDiasByOfficeId(officeId: Int): Flow<List<DiaEntity>>

    @Query("SELECT * FROM dias WHERE officeName = :officeName")
    fun getDiasByOfficeName(officeName: String): Flow<List<DiaEntity>>

    @Query("SELECT * FROM dias WHERE id = :id")
    suspend fun getDiaById(id: Long): DiaEntity?

    @Query("SELECT * FROM dias WHERE diaId = :diaId AND officeName = :officeName")
    suspend fun getDiaByDiaIdAndOffice(diaId: String, officeName: String): DiaEntity?

    @Query("SELECT * FROM dias WHERE diaId = :diaId AND officeName = :officeName AND typeName = :typeName")
    suspend fun getDiaByDiaIdAndOfficeAndType(diaId: String, officeName: String, typeName: String): DiaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dias: List<DiaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dia: DiaEntity)

    @Query("DELETE FROM dias")
    suspend fun deleteAll()

    @Query("DELETE FROM dias WHERE officeId = :officeId")
    suspend fun deleteByOfficeId(officeId: Int)

    @Query("SELECT COUNT(*) FROM dias")
    suspend fun getCount(): Int

    @Update
    suspend fun update(dia: DiaEntity)

    @Query("SELECT COUNT(*) FROM dias WHERE officeId = :officeId")
    suspend fun getCountByOfficeId(officeId: Int): Int
}
