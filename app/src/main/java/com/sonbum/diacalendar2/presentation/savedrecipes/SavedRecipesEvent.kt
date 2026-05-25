package com.sonbum.diacalendar2.presentation.savedrecipes

sealed interface SavedRecipesEvent {
	data class NavigateToDetails(val recipeId: Int) : SavedRecipesEvent
}