package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.UserShiftConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserShiftConfigDao {

    @Query("SELECT * FROM user_shift_config WHERE id = 1")
    fun getConfig(): Flow<UserShiftConfigEntity?>

    @Query("SELECT * FROM user_shift_config WHERE id = 1")
    suspend fun getConfigOnce(): UserShiftConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: UserShiftConfigEntity)

    @Query("DELETE FROM user_shift_config")
    suspend fun deleteConfig()
}
