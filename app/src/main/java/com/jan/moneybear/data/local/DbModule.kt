package com.jan.moneybear.data.local

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room

fun provideDb(context: Context): AppDb {
    val builder = Room.databaseBuilder(context, AppDb::class.java, "moneybear.db")

    val isDebuggable =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    if (isDebuggable) {
        builder.fallbackToDestructiveMigration()
    }

    return builder.build()
}

