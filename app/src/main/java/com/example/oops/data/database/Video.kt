package com.example.oops.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video")
class Video(var videoUrl: String? = null,
            var watchedLength: Long? = null) {

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}