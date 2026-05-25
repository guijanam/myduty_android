package com.sonbum.diacalendar2.presentation.savedrecipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SavedRecipesScreen(
	modifier: Modifier = Modifier,
	onAction: (SavedRecipesAction) -> Unit = {},
	state: SavedRecipesState,
) {
	val savedRecipes = state.savedRecipes

	LazyColumn(
		modifier = modifier
			.padding(bottom = 10.dp),
		contentPadding = PaddingValues(16.dp),
	) {
		items(savedRecipes.size) { id ->
			Text(
				text = "Saved Recipes ${savedRecipes[id]}",
				modifier = Modifier
					.fillMaxWidth()
					.clickable {
						onAction(SavedRecipesAction.OnRecipeClick(id))
					}
					.padding(16.dp)
			)
		}
	}
}