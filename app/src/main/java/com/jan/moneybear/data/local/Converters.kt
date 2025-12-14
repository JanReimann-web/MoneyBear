package com.jan.moneybear.data.local

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromTxType(value: TxType?): String? = value?.name

    @TypeConverter
    fun toTxType(value: String?): TxType? = value?.let { TxType.valueOf(it) }
}

