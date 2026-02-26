package com.ercan.smartwatch.data.model

data class UserSettings(
    val city: String = "",
    val country: String = "",
    val methodId: Int = DEFAULT_METHOD_ID,
    val schoolId: Int = DEFAULT_SCHOOL_ID,
    val calendarMethod: String = DEFAULT_CALENDAR_METHOD
) {
    fun isConfigured(): Boolean = city.isNotBlank() && country.isNotBlank()

    fun settingsHash(): String {
        return listOf(city.trim().lowercase(), country.trim().lowercase(), methodId, schoolId, calendarMethod)
            .joinToString("|")
    }

    companion object {
        const val DEFAULT_METHOD_ID: Int = 13
        const val DEFAULT_SCHOOL_ID: Int = 1
        const val DEFAULT_CALENDAR_METHOD: String = "ANGLE_BASED"
    }
}
