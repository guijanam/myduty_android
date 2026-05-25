package com.sonbum.diacalendar2.presentation.recipedetails

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecipeDetailsViewModel(
	recipeId: Int,
) : ViewModel() {

	private val _recipeId = MutableStateFlow(recipeId)
	val recipeId = _recipeId.asStateFlow()

	init {
		println("RecipeDetailsViewModel created")
	}

	override fun onCleared() {
		println("RecipeDetailsViewModel cleared")
		super.onCleared()
	}
}