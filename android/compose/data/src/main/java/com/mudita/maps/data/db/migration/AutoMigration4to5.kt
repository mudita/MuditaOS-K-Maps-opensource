package com.mudita.maps.data.db.migration

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(
    tableName = "search_history",
    columnName = "search_distance"
)
class AutoMigration4to5 : AutoMigrationSpec
