package com.example.alakey.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fact: FactEntity)

    @Query("SELECT * FROM facts WHERE entityId = :entityId")
    suspend fun getFactsUsingEntity(entityId: String): List<FactEntity>

    @Query("SELECT * FROM facts WHERE attribute = :attribute")
    suspend fun getFactsUsingAttribute(attribute: String): List<FactEntity>
    
    @Query("SELECT * FROM facts")
    suspend fun getAllFacts(): List<FactEntity>
    
    // Datomic-style: Point-in-time query capability could go here eventually
}
