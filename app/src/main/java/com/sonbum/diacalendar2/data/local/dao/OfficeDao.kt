package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.sonbum.diacalendar2.data.local.entity.OfficeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficeDao {

    @Query("SELECT * FROM offices")
    fun getAllOffices(): Flow<List<OfficeEntity>>

    @Query("SELECT * FROM offices WHERE officeCode = :officeCode")
    suspend fun getOfficeByCode(officeCode: Long): OfficeEntity?

    @Query("SELECT * FROM offices WHERE officeName = :officeName")
    suspend fun getOfficeByName(officeName: String): OfficeEntity?

    @Upsert
    suspend fun insertAll(offices: List<OfficeEntity>)

    @Upsert
    suspend fun insert(office: OfficeEntity)

    @Query("DELETE FROM offices")
    suspend fun deleteAll()

    @Update
    suspend fun update(office: OfficeEntity)

    @Query("SELECT COUNT(*) FROM offices")
    suspend fun getCount(): Int
}
