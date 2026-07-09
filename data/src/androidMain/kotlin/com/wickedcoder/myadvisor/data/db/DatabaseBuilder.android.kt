package com.wickedcoder.myadvisor.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun appDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val dbFile = context.getDatabasePath(DATABASE_NAME)
    return Room.databaseBuilder<AppDatabase>(context, dbFile.absolutePath)
}
