package com.wickedcoder.myadvisor.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun appDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val documentsDir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return Room.databaseBuilder<AppDatabase>(
        name = requireNotNull(documentsDir?.path) + "/" + DATABASE_NAME,
    )
}
