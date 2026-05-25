package com.sonbum.diacalendar2.presentation.savedrecipes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SavedRecipesRoot(
	onNavigateToDetails: (id: Int) -> Unit,
	viewModel: SavedRecipesViewModel = viewModel(),
) {
	val state by viewModel.state.collectAsStateWithLifecycle()

	LaunchedEffect(viewModel.event) {
		viewModel.event.collect { event ->
			when (event) {
				// 화면 이동
				is SavedRecipesEvent.NavigateToDetails -> {
					onNavigateToDetails(event.recipeId)
				}
				// SnackBar
				// Dialog 등등
			}
		}
	}

	SavedRecipesScreen(
		state = state,
		onAction = viewModel::onAction,
	)
}