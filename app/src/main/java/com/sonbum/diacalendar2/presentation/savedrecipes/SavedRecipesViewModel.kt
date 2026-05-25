package com.sonbum.diacalendar2.presentation.savedrecipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SavedRecipesViewModel : ViewModel() {
	private val _state = MutableStateFlow(SavedRecipesState())
	val state = _state.asStateFlow()

	private val _event = MutableSharedFlow<SavedRecipesEvent>()
	val event = _event.asSharedFlow()

	init {
		val savedRecipes = (0..20).toList()
			.map { "Recipe $it" }

		_state.update {
			it.copy(savedRecipes = savedRecipes)
		}
	}

	fun onAction(action: SavedRecipesAction) {
		when (action) {
			is SavedRecipesAction.OnRecipeClick -> {
				viewModelScope.launch {
					_event.emit(SavedRecipesEvent.NavigateToDetails(action.recipeId))
				}
			}
		}
	}
}