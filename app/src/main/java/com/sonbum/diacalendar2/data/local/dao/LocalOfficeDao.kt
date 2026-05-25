package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonbum.diacalendar2.data.local.entity.LocalOfficeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalOfficeDao {

    @Query("SELECT * FROM local_offices ORDER BY createdAt DESC")
    fun getAllLocalOffices(): Flow<List<LocalOfficeEntity>>

    @Query("SELECT * FROM local_offices WHERE id = :id")
    suspend fun getLocalOfficeById(id: Long): LocalOfficeEntity?

    @Query("SELECT * FROM local_offices WHERE officeName = :name")
    suspend fun getLocalOfficeByName(name: String): LocalOfficeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(office: LocalOfficeEntity): Long

    @Update
    suspend fun update(office: LocalOfficeEntity)

    @Query("DELETE FROM local_offices WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM local_offices")
    suspend fun getCount(): Int

    @Query("SELECT * FROM local_offices ORDER BY createdAt DESC")
    fun getAllOffices(): Flow<List<LocalOfficeEntity>>

    @Query("SELECT * FROM local_offices WHERE officeName = :name LIMIT 1")
    suspend fun getOfficeByName(name: String): LocalOfficeEntity?

    @Query("DELETE FROM local_offices")
    suspend fun deleteAll()
}
