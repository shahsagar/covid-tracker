package com.sagar.assignment1

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SignDao {
    @Insert
    suspend fun insert(sign: Sign)

    @Query("SELECT count(1) FROM sign")
    fun selectAll(): LiveData<Integer>

    @Query("SELECT * FROM sign ORDER BY 1 DESC LIMIT 1")
    fun latestEntry(): LiveData<Sign>
}