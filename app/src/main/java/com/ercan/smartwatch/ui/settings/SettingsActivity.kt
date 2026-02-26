package com.ercan.smartwatch.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.ercan.smartwatch.ServiceLocator
import com.ercan.smartwatch.data.model.CalculationMethod
import com.ercan.smartwatch.data.model.UserSettings
import com.ercan.smartwatch.data.repo.PrayerTimesRepository
import com.ercan.smartwatch.data.store.SettingsStore
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsStore = ServiceLocator.settingsStore(this)
        val repository = ServiceLocator.prayerRepository(this)

        setContent {
            MaterialTheme {
                SettingsScreen(
                    settingsStore = settingsStore,
                    repository = repository
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settingsStore: SettingsStore,
    repository: PrayerTimesRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    var isLoading by remember { mutableStateOf(true) }
    var methods by remember { mutableStateOf(emptyList<CalculationMethod>()) }
    var city by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var methodId by rememberSaveable { mutableStateOf(UserSettings.DEFAULT_METHOD_ID) }
    var schoolId by rememberSaveable { mutableStateOf(UserSettings.DEFAULT_SCHOOL_ID) }
    var calendarMethod by rememberSaveable { mutableStateOf(UserSettings.DEFAULT_CALENDAR_METHOD) }

    var editingField by remember { mutableStateOf<EditableField?>(null) }
    var draftValue by rememberSaveable { mutableStateOf("") }

    val schoolOptions = remember { listOf(0 to "Shafi", 1 to "Hanafi") }
    val calendarMethodOptions = remember {
        listOf("ANGLE_BASED", "MIDDLE_OF_THE_NIGHT", "ONE_SEVENTH")
    }

    fun normalizeSelections(availableMethods: List<CalculationMethod>) {
        if (availableMethods.none { it.id == methodId }) {
            methodId = availableMethods.firstOrNull { it.id == UserSettings.DEFAULT_METHOD_ID }?.id
                ?: availableMethods.firstOrNull()?.id
                ?: UserSettings.DEFAULT_METHOD_ID
        }

        if (schoolOptions.none { it.first == schoolId }) {
            schoolId = UserSettings.DEFAULT_SCHOOL_ID
        }

        if (calendarMethod !in calendarMethodOptions) {
            calendarMethod = UserSettings.DEFAULT_CALENDAR_METHOD
        }
    }

    LaunchedEffect(Unit) {
        val saved = settingsStore.get()
        city = saved.city
        country = saved.country
        methodId = saved.methodId
        schoolId = saved.schoolId
        calendarMethod = saved.calendarMethod

        methods = repository.getCalculationMethods()
        normalizeSelections(methods)

        isLoading = false

        launch {
            val refreshed = runCatching { repository.refreshCalculationMethods() }
                .getOrElse { return@launch }
            methods = refreshed
            normalizeSelections(methods)
        }
    }

    AppScaffold(timeText = { TimeText() }) {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@ScreenScaffold
            }

            val selectedMethodLabel = methods.firstOrNull { it.id == methodId }?.name ?: "Tap to choose"
            val selectedSchoolLabel = schoolOptions.firstOrNull { it.first == schoolId }?.second ?: "Tap to choose"

            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = contentPadding
            ) {
                item {
                    ListHeader {
                        Text("Watch Settings")
                    }
                }

                item {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            editingField = EditableField.City
                            draftValue = city
                        },
                        secondaryLabel = { Text(city.ifBlank { "Tap to edit" }, maxLines = 1) }
                    ) {
                        Text("City")
                    }
                }

                item {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            editingField = EditableField.Country
                            draftValue = country
                        },
                        secondaryLabel = { Text(country.ifBlank { "Tap to edit" }, maxLines = 1) }
                    ) {
                        Text("Country")
                    }
                }

                item {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val methodIds = methods.map { it.id }
                            methodId = nextIntOption(methodId, methodIds)
                        },
                        secondaryLabel = { Text(selectedMethodLabel, maxLines = 1) }
                    ) {
                        Text("Method")
                    }
                }

                item {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            schoolId = nextIntOption(schoolId, schoolOptions.map { it.first })
                        },
                        secondaryLabel = { Text(selectedSchoolLabel, maxLines = 1) }
                    ) {
                        Text("School")
                    }
                }

                item {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            calendarMethod = nextStringOption(calendarMethod, calendarMethodOptions)
                        },
                        secondaryLabel = { Text(calendarMethod, maxLines = 1) }
                    ) {
                        Text("Calendar Method")
                    }
                }

                item {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            scope.launch {
                                if (city.isBlank() || country.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "City and country are required",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }

                                settingsStore.save(
                                    UserSettings(
                                        city = city.trim(),
                                        country = country.trim(),
                                        methodId = methodId,
                                        schoolId = schoolId,
                                        calendarMethod = calendarMethod
                                    )
                                )

                                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                            }
                        },
                        secondaryLabel = { Text("Apply to watch face") }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    if (editingField != null) {
        val isCity = editingField == EditableField.City
        AlertDialog(
            onDismissRequest = { editingField = null },
            title = { M3Text(if (isCity) "City" else "Country") },
            text = {
                OutlinedTextField(
                    value = draftValue,
                    onValueChange = { draftValue = it },
                    singleLine = true,
                    label = { M3Text(if (isCity) "City" else "Country") }
                )
            },
            confirmButton = {
                M3TextButton(
                    onClick = {
                        val value = draftValue.trim()
                        if (isCity) {
                            city = value
                        } else {
                            country = value
                        }
                        editingField = null
                    }
                ) {
                    M3Text("OK")
                }
            },
            dismissButton = {
                M3TextButton(onClick = { editingField = null }) {
                    M3Text("Cancel")
                }
            }
        )
    }
}

private enum class EditableField {
    City,
    Country
}

private fun nextIntOption(current: Int, options: List<Int>): Int {
    if (options.isEmpty()) return current
    val currentIndex = options.indexOf(current).takeIf { it >= 0 } ?: -1
    return options[(currentIndex + 1) % options.size]
}

private fun nextStringOption(current: String, options: List<String>): String {
    if (options.isEmpty()) return current
    val currentIndex = options.indexOf(current).takeIf { it >= 0 } ?: -1
    return options[(currentIndex + 1) % options.size]
}
