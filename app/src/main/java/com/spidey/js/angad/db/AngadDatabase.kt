//will create a local database ( as angad_databadse)!
package com.spidey.js.angad.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DnsEvent::class], version = 4, exportSchema = false)
abstract class AngadDatabase : RoomDatabase() {
    abstract fun dnsEventDao(): DnsEventDao

    companion object {
        @Volatile
        private var INSTANCE: AngadDatabase? = null

        fun getDatabase(context: Context): AngadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AngadDatabase::class.java,
                    "angad_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
