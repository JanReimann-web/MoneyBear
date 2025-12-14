package com.jan.moneybear.data.local

import android.content.Context
import androidx.room.Room

fun provideDb(context: Context): AppDb =
    Room.databaseBuilder(context, AppDb::class.java, "moneybear.db")
        .fallbackToDestructiveMigration()
        .build()

