package com.sonbum.diacalendar2.presentation.savedrecipes

sealed interface SavedRecipesAction {
	data class OnRecipeClick(val recipeId: Int) : SavedRecipesAction
}