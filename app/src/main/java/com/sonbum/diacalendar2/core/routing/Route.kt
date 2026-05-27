package com.sonbum.diacalendar2.core.routing

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
	@Serializable
	data object SignIn : Route

	@Serializable
	data object Main : Route

	@Serializable
	data object Home : Route

	@Serializable
	data object SavedRecipes : Route

	@Serializable
	data object Notifications : Route

	@Serializable
	data object Profile : Route

	@Serializable
	data class DateDetail(val dateString: String, val openEventDialog: Boolean = false) : Route

	@Serializable
	data class MemoEdit(val dateString: String, val memoId: String? = null) : Route

	@Serializable
	data object CalendarSelection : Route

	@Serializable
	data object ShiftSelection : Route

	@Serializable
	data class RecipeDetail(val recipeId: Int) : Route

	@Serializable
	data object DiaTable : Route

	@Serializable
	data object VacationSetting : Route

	@Serializable
	data object LocalOfficeList : Route

	@Serializable
	data class LocalOfficeEdit(val officeId: Long? = null) : Route

	@Serializable
	data class LocalDiaList(val officeId: Long) : Route

	@Serializable
	data class LocalDiaEdit(val officeId: Long, val diaId: Long? = null) : Route

	@Serializable
	data object CustomShiftList : Route

	@Serializable
	data class CustomShiftEdit(val shiftId: Long? = null) : Route

	@Serializable
	data object TextSizeSettings : Route

	@Serializable
	data object Auth : Route

	@Serializable
	data object NicknameSetup : Route

	@Serializable
	data class PostDetail(val postId: Long) : Route

	@Serializable
	data class PostWrite(val category: String? = null) : Route

	@Serializable
	data class PostEdit(val postId: Long) : Route

	@Serializable
	data object BlockedUsers : Route

	@Serializable
	data object Community : Route

	@Serializable
	data class ServerDiaEdit(val diaId: Long) : Route

	@Serializable
	data class ServerOfficeEdit(val officeCode: Long) : Route

	@Serializable
	data class Menu(val dateString: String) : Route

	@Serializable
	data class OfficeWebsite(val url: String, val officeName: String) : Route

	@Serializable
	data object OfficeWebsiteTab : Route

	@Serializable
	data object Coworker : Route

	@Serializable
	data class DocumentDetail(val documentId: String) : Route

	@Serializable
	data object Anniversary : Route

	@Serializable
	data object CoworkerGroup : Route

	@Serializable
	data class CoworkerEdit(val coworkerId: Long? = null) : Route

	companion object Companion {
		val allRoutes: List<Route> = listOf(Home, SavedRecipes, Notifications, Community, Profile)
	}
}