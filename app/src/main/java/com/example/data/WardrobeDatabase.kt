package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.WardrobeDao
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity

@Database(
    entities = [GarmentEntity::class, ReportEntity::class, JunctionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class WardrobeDatabase : RoomDatabase() {
    abstract fun wardrobeDao(): WardrobeDao

    companion object {
        @Volatile
        private var INSTANCE: WardrobeDatabase? = null

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clothes ADD COLUMN sideBreastGatherBra TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN sideBreastSideOutBra TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN aircondHotFeeling TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN aircondHotExplain TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN aircondLegFeeling TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN aircondLegExplain TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN noAircondHotFeeling TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN noAircondHotExplain TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN noAircondLegFeeling TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN noAircondLegExplain TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN fanSleepHotFeeling TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN fanSleepHotExplain TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN fanSleepLegFeeling TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothes ADD COLUMN fanSleepLegExplain TEXT NOT NULL DEFAULT ''")

                // Migrate historical Hot at noon to noAircondHotFeeling
                db.execSQL("UPDATE clothes SET noAircondHotFeeling = hotAtNoon WHERE (noAircondHotFeeling IS NULL OR noAircondHotFeeling = '') AND (hotAtNoon IS NOT NULL AND hotAtNoon != '')")
                // Migrate historical sideBreastOk to sideBreastGatherBra
                db.execSQL("UPDATE clothes SET sideBreastGatherBra = sideBreastOk WHERE (sideBreastGatherBra IS NULL OR sideBreastGatherBra = '') AND (sideBreastOk IS NOT NULL AND sideBreastOk != '')")
            }
        }

        fun getDatabase(context: Context): WardrobeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WardrobeDatabase::class.java,
                    "wardrobe_database"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
