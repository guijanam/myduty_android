package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.SubShiftConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubShiftConfigDao {

    @Query("SELECT * FROM sub_shift_config WHERE id = 1")
    fun getConfig(): Flow<SubShiftConfigEntity?>

    @Query("SELECT * FROM sub_shift_config WHERE id = 1")
    suspend fun getConfigOnce(): SubShiftConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: SubShiftConfigEntity)

    @Query("DELETE FROM sub_shift_config")
    suspend fun deleteConfig()
}
