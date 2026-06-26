package com.softartdev.kvace.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KvaceScreenScaffold(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable (PaddingValues) -> Unit,
) = Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
        TopAppBar(title = { Text(text = title, style = MaterialTheme.typography.titleLarge) })
    },
    content = content,
)
