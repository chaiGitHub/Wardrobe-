package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrobeDao {
    // Clothes
    @Query("SELECT * FROM clothes ORDER BY name ASC")
    fun getAllClothes(): Flow<List<GarmentEntity>>

    @Query("SELECT * FROM clothes ORDER BY name ASC")
    suspend fun getAllClothesSnapshot(): List<GarmentEntity>

    @Query("SELECT * FROM clothes WHERE id = :id LIMIT 1")
    suspend fun getGarmentById(id: String): GarmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGarment(garment: GarmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClothes(garments: List<GarmentEntity>)

    @Update
    suspend fun updateGarment(garment: GarmentEntity)

    @Query("DELETE FROM clothes WHERE id = :id")
    suspend fun deleteGarment(id: String)

    @Query("DELETE FROM clothes")
    suspend fun clearClothes()

    @Query("SELECT COUNT(*) FROM clothes")
    suspend fun getClothesCount(): Int

    // Reports
    @Query("SELECT * FROM reports ORDER BY wearDate DESC, created DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports ORDER BY wearDate DESC, created DESC")
    suspend fun getAllReportsSnapshot(): List<ReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<ReportEntity>)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteReport(id: String)

    @Query("DELETE FROM reports")
    suspend fun clearReports()

    // Junctions
    @Query("SELECT * FROM junctions ORDER BY wearDate DESC")
    fun getAllJunctions(): Flow<List<JunctionEntity>>

    @Query("SELECT * FROM junctions ORDER BY wearDate DESC")
    suspend fun getAllJunctionsSnapshot(): List<JunctionEntity>

    @Query("SELECT * FROM junctions WHERE wearingReportId = :reportId")
    fun getJunctionsForReport(reportId: String): Flow<List<JunctionEntity>>

    @Query("SELECT * FROM junctions WHERE clothesId = :clothesId")
    fun getJunctionsForClothes(clothesId: String): Flow<List<JunctionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJunction(junction: JunctionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJunctions(junctions: List<JunctionEntity>)

    @Update
    suspend fun updateJunction(junction: JunctionEntity)

    @Query("UPDATE junctions SET washStated = :status, washDate = :date, washedDate = :date WHERE id = :id")
    suspend fun updateWashStatus(id: String, status: String, date: String)

    @Query("DELETE FROM junctions WHERE id = :id")
    suspend fun deleteJunction(id: String)

    @Query("DELETE FROM junctions WHERE wearingReportId = :reportId")
    suspend fun deleteJunctionsForReport(reportId: String)

    @Query("DELETE FROM junctions")
    suspend fun clearJunctions()
}
