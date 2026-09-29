package com.sonbum.diacalendar2.presentation.anniversary

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.datastore.BirthdayDefaults
import com.sonbum.diacalendar2.data.local.datastore.BirthdayPreferences
import com.sonbum.diacalendar2.domain.model.AgeSnapshot
import com.sonbum.diacalendar2.domain.model.BirthdayGroup
import com.sonbum.diacalendar2.domain.model.BirthdayMilestone
import com.sonbum.diacalendar2.domain.model.BirthdayOccurrence
import com.sonbum.diacalendar2.domain.model.BirthdayPerson
import com.sonbum.diacalendar2.domain.model.MilestoneOccurrence
import com.sonbum.diacalendar2.domain.repository.BirthdayRepository
import com.sonbum.diacalendar2.domain.repository.DeviceCalendarRepository
import com.sonbum.diacalendar2.domain.usecase.BirthdayCalendarSyncUseCase
import com.sonbum.diacalendar2.domain.util.AgeCalculator
import com.sonbum.diacalendar2.domain.util.BirthdayDateResolver
import com.sonbum.diacalendar2.domain.util.occurrence
import com.sonbum.diacalendar2.domain.model.DeviceCalendar
import com.sonbum.diacalendar2.widget.WidgetUpdater
import com.sonbum.diacalendar2.core.notification.BirthdayReminderScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

data class BirthdayCardUi(
    val person: BirthdayPerson,
    val occurrence: BirthdayOccurrence,
    val daysUntil: Long,
    val ageText: String
)

data class BirthdayDashboardState(
    val people: List<BirthdayPerson> = emptyList(),
    val groups: List<BirthdayGroup> = emptyList(),
    val today: List<BirthdayCardUi> = emptyList(),
    val upcoming: List<BirthdayCardUi> = emptyList(),
    val thisMonth: List<BirthdayCardUi> = emptyList(),
    val milestones: List<MilestoneOccurrence> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface BirthdayEvent {
    data class Message(val text: String) : BirthdayEvent
    data object Saved : BirthdayEvent
}

class BirthdayViewModel(
    private val repository: BirthdayRepository,
    private val dateResolver: BirthdayDateResolver,
    private val ageCalculator: AgeCalculator,
    private val preferences: BirthdayPreferences,
    private val deviceCalendarRepository: DeviceCalendarRepository,
    private val calendarSyncUseCase: BirthdayCalendarSyncUseCase,
    private val reminderScheduler: BirthdayReminderScheduler,
    private val appContext: Context
) : ViewModel() {
    private val _state = MutableStateFlow(BirthdayDashboardState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<BirthdayEvent>()
    val event = _event.asSharedFlow()

    val defaults = preferences.defaults.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), BirthdayDefaults()
    )

    private val _deviceCalendars = MutableStateFlow<List<DeviceCalendar>>(emptyList())
    val deviceCalendars = _deviceCalendars.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaults()
            calendarSyncUseCase.sync()
            reminderScheduler.rescheduleAll()
            combine(repository.observePeople(), repository.observeGroups()) { people, groups -> people to groups }
                .collect { (people, groups) -> rebuildDashboard(people, groups) }
        }
    }

    fun ageSnapshot(person: BirthdayPerson): AgeSnapshot? = ageCalculator.snapshot(person)
    fun ageText(person: BirthdayPerson): String = ageCalculator.displayText(person)
    fun zodiac(person: BirthdayPerson): String? = ageCalculator.zodiac(person)
    fun westernZodiac(person: BirthdayPerson): String? = ageCalculator.westernZodiac(person)
    fun solarBirthDate(person: BirthdayPerson): LocalDate? = dateResolver.solarBirthDate(person)
    fun nextBirthday(person: BirthdayPerson): LocalDate? =
        dateResolver.nextBirthday(person, ageCalculator.todayFor(person))

    suspend fun getPerson(id: Long): BirthdayPerson? = repository.getPerson(id)
    fun observeMilestones(personId: Long) = repository.observeMilestones(personId)

    fun savePerson(person: BirthdayPerson, photoUri: Uri? = null) {
        viewModelScope.launch {
            var copiedPhotoPath: String? = null
            try {
                val photoPath = photoUri?.let(::copyProfilePhoto)?.also { copiedPhotoPath = it } ?: person.photoPath
                val savedId = repository.savePerson(person.copy(photoPath = photoPath))
                calendarSyncUseCase.sync()
                reminderScheduler.scheduleNext(savedId)
                WidgetUpdater.updateAll(appContext)
                _event.emit(BirthdayEvent.Saved)
            } catch (e: Exception) {
                copiedPhotoPath?.let { runCatching { File(it).delete() } }
                _event.emit(BirthdayEvent.Message("저장하지 못했습니다: ${e.message}"))
            }
        }
    }

    fun deletePerson(id: Long) {
        viewModelScope.launch {
            reminderScheduler.cancel(id)
            repository.deletePerson(id)
            calendarSyncUseCase.sync()
            WidgetUpdater.updateAll(appContext)
            _event.emit(BirthdayEvent.Saved)
        }
    }

    fun saveGroup(group: BirthdayGroup) {
        viewModelScope.launch {
            runCatching { repository.saveGroup(group) }
                .onFailure { _event.emit(BirthdayEvent.Message("같은 이름의 그룹이 이미 있습니다")) }
        }
    }

    fun deleteGroup(group: BirthdayGroup) {
        if (group.isDefault) return
        viewModelScope.launch { repository.deleteCustomGroup(group.id) }
    }

    fun saveMilestone(milestone: BirthdayMilestone) {
        viewModelScope.launch {
            repository.saveMilestone(milestone)
            reminderScheduler.scheduleNext(milestone.personId)
            rebuildDashboard(_state.value.people, _state.value.groups)
        }
    }

    fun deleteMilestone(milestone: BirthdayMilestone) {
        viewModelScope.launch {
            repository.deleteMilestone(milestone.id)
            reminderScheduler.scheduleNext(milestone.personId)
            rebuildDashboard(_state.value.people, _state.value.groups)
        }
    }

    fun loadDeviceCalendars() {
        viewModelScope.launch {
            _deviceCalendars.value = runCatching { deviceCalendarRepository.getCalendars().filter { it.isWritable } }
                .getOrDefault(emptyList())
        }
    }

    fun saveDefaults(defaults: BirthdayDefaults) {
        viewModelScope.launch {
            val previous = preferences.defaults.first()
            if (previous.calendarId > 0 && previous.calendarId != defaults.calendarId) {
                calendarSyncUseCase.clear(previous.calendarId)
            }
            preferences.saveDefaults(defaults)
            if (defaults.calendarSyncEnabled) calendarSyncUseCase.sync() else calendarSyncUseCase.clear()
            reminderScheduler.rescheduleAll()
            _event.emit(BirthdayEvent.Message("생일 설정을 저장했습니다"))
        }
    }

    private suspend fun rebuildDashboard(people: List<BirthdayPerson>, groups: List<BirthdayGroup>) {
        val cards = people.mapNotNull { person ->
            val today = ageCalculator.todayFor(person)
            val occurrence = dateResolver.nextBirthday(person, today)?.let { date ->
                val yearCandidate = (today.year - 1..today.year + 8)
                    .mapNotNull { dateResolver.occurrence(person, it) }
                    .firstOrNull { it.date == date }
                yearCandidate
            } ?: return@mapNotNull null
            BirthdayCardUi(
                person = person,
                occurrence = occurrence,
                daysUntil = ChronoUnit.DAYS.between(today, occurrence.date),
                ageText = ageCalculator.displayText(person)
            )
        }.sortedWith(compareBy<BirthdayCardUi> { it.daysUntil }.thenBy { it.person.name })

        val deviceToday = LocalDate.now(ZoneId.systemDefault())
        val monthCards = people.mapNotNull { person ->
            val occurrence = (deviceToday.year - 1..deviceToday.year + 1)
                .mapNotNull { dateResolver.occurrence(person, it) }
                .firstOrNull { it.date.year == deviceToday.year && it.date.month == deviceToday.month }
                ?: return@mapNotNull null
            val personToday = ageCalculator.todayFor(person)
            BirthdayCardUi(
                person = person,
                occurrence = occurrence,
                daysUntil = ChronoUnit.DAYS.between(personToday, occurrence.date),
                ageText = ageCalculator.displayText(person)
            )
        }.sortedBy { it.occurrence.date }
        val milestoneEnd = deviceToday.plusYears(5)
        val milestones = repository.getMilestoneOccurrences()
            .filter { !it.date.isBefore(deviceToday) && !it.date.isAfter(milestoneEnd) }
        _state.update {
            BirthdayDashboardState(
                people = people,
                groups = groups,
                today = cards.filter { it.daysUntil == 0L },
                upcoming = cards.filter { it.daysUntil in 1..366 },
                thisMonth = monthCards,
                milestones = milestones,
                isLoading = false
            )
        }
    }

    private fun copyProfilePhoto(uri: Uri): String {
        val dir = File(appContext.filesDir, "birthday_profiles").apply { mkdirs() }
        val destination = File(dir, "${UUID.randomUUID()}.jpg")
        appContext.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "사진을 읽을 수 없습니다" }
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        return destination.absolutePath
    }
}
