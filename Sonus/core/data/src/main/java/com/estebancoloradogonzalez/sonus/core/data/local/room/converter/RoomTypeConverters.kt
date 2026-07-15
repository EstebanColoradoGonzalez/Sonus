package com.estebancoloradogonzalez.sonus.core.data.local.room.converter

import androidx.room.TypeConverter
import com.estebancoloradogonzalez.sonus.core.domain.model.ContentType
import com.estebancoloradogonzalez.sonus.core.domain.model.ThemePreference
import com.estebancoloradogonzalez.sonus.core.domain.model.TrackAvailability

/**
 * Room converters for the domain enums used by `Track` and `AppSettings`. Enums are persisted by
 * **stable name**, never by ordinal (coding-standards §4.2), so adding a value never shifts existing
 * rows.
 */
class RoomTypeConverters {
    @TypeConverter
    fun contentTypeToName(value: ContentType): String = value.name

    @TypeConverter
    fun nameToContentType(value: String): ContentType = ContentType.valueOf(value)

    @TypeConverter
    fun availabilityToName(value: TrackAvailability): String = value.name

    @TypeConverter
    fun nameToAvailability(value: String): TrackAvailability = TrackAvailability.valueOf(value)

    @TypeConverter
    fun themePreferenceToName(value: ThemePreference): String = value.name

    @TypeConverter
    fun nameToThemePreference(value: String): ThemePreference = ThemePreference.valueOf(value)
}
