package com.sonbum.diacalendar2.presentation.recipedetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RecipeDetailsScreen(
	id: Int,
	modifier: Modifier = Modifier,
	viewModel: RecipeDetailsViewModel = viewModel {
		RecipeDetailsViewModel(id)
	},
) {
	val recipeId by viewModel.recipeId.collectAsStateWithLifecycle()

	Box(
		modifier = modifier
			.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Text("RecipeDetailsScreen $recipeId")
	}
}