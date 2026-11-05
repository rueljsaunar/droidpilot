package com.droidpilot.feature.objectrepository.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.droidpilot.core.automation.RepositoryEntry
import com.droidpilot.core.automation.Selector

@Entity(tableName = "repository_entries")
internal data class RepositoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "selector_raw") val selectorRaw: String,
    @ColumnInfo(name = "selector_type") val selectorType: String,
    @ColumnInfo(name = "app_package") val appPackage: String,
    @ColumnInfo(name = "screen_label") val screenLabel: String,
    @ColumnInfo(name = "screenshot_path") val screenshotPath: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
) {
    fun toDomain(): RepositoryEntry = RepositoryEntry(
        id = id,
        name = name,
        selector = SelectorConverter.deserialize(selectorRaw),
        selectorType = selectorType,
        appPackage = appPackage,
        screenLabel = screenLabel,
        screenshotPath = screenshotPath,
        createdAt = createdAt,
    )

    companion object {
        fun from(entry: RepositoryEntry) = RepositoryEntryEntity(
            id = entry.id,
            name = entry.name,
            selectorRaw = SelectorConverter.serialize(entry.selector),
            selectorType = entry.selectorType,
            appPackage = entry.appPackage,
            screenLabel = entry.screenLabel,
            screenshotPath = entry.screenshotPath,
            createdAt = entry.createdAt,
        )
    }
}
