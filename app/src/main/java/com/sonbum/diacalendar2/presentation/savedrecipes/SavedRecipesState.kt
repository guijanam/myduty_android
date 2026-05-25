package com.sonbum.diacalendar2.presentation.savedrecipes

import androidx.compose.runtime.Immutable

@Immutable
data class SavedRecipesState(
	val savedRecipes: List<String> = emptyList(),
	val isLoading: Boolean = false,
)