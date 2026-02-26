package com.example.exspensify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.exspensify.core.util.UiEvent
import com.example.exspensify.ui.categories.addedits.AddEditCategoryViewModel
import com.example.exspensify.ui.categories.addedits.AddEditCategoryEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryScreen(
    viewModel: AddEditCategoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.NavigateBack -> {
                    onNavigateBack()
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.id == null) "Add Category" else "Edit Category"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Disabled message for default categories
            if (uiState.isDefault) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Default categories cannot be edited",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Name Field
            OutlinedTextField(
                value = uiState.name,
                onValueChange = {
                    viewModel.onEvent(AddEditCategoryEvent.NameChanged(it))
                },
                label = { Text("Category Name") },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                enabled = !uiState.isDefault,
                modifier = Modifier.fillMaxWidth()
            )

            // Icon Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Icon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconSelector(
                    selectedIcon = uiState.selectedIcon,
                    onIconSelected = {
                        viewModel.onEvent(AddEditCategoryEvent.IconSelected(it))
                    },
                    enabled = !uiState.isDefault
                )
            }

            // Color Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ColorSelector(
                    selectedColor = uiState.selectedColor,
                    onColorSelected = {
                        viewModel.onEvent(AddEditCategoryEvent.ColorSelected(it))
                    },
                    enabled = !uiState.isDefault
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = { viewModel.onEvent(AddEditCategoryEvent.SaveCategory) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && !uiState.isDefault
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.id == null) "Create" else "Save")
                }
            }
        }
    }
}

@Composable
fun IconSelector(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    enabled: Boolean
) {
    val icons = listOf(
        "🍔", "🍕", "☕", "🍎", "🥗", "🍜",
        "🚗", "🚕", "🚌", "🚇", "✈️", "🚲",
        "🏠", "🏢", "🏥", "🏫", "🏪", "🏨",
        "🎬", "🎮", "🎵", "🎨", "📚", "⚽",
        "💼", "💰", "💳", "💡", "🔧", "🛍️",
        "💊", "🏃", "💪", "🎁", "📱", "💻"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(200.dp)
    ) {
        items(icons) { icon ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (icon == selectedIcon)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = 2.dp,
                        color = if (icon == selectedIcon)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable(enabled = enabled) { onIconSelected(icon) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
fun ColorSelector(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    enabled: Boolean
) {
    val colors = listOf(
        0xFFEF5350L, 0xFFEC407AL, 0xFFAB47BCL, 0xFF7E57C2L,
        0xFF5C6BC0L, 0xFF42A5F5L, 0xFF29B6F6L, 0xFF26C6DAL,
        0xFF26A69AL, 0xFF66BB6AL, 0xFF9CCC65L, 0xFFD4E157L,
        0xFFFFEE58L, 0xFFFFCA28L, 0xFFFF7043L, 0xFF8D6E63L,
        0xFF78909CL, 0xFF9E9E9EL
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = 3.dp,
                        color = if (color == selectedColor)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable(enabled = enabled) { onColorSelected(color) }
            )
        }
    }
}