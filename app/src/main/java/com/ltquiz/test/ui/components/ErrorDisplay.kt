package com.ltquiz.test.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ltquiz.test.errors.ErrorMessage
import com.ltquiz.test.ui.viewmodels.ErrorViewModel
import kotlinx.coroutines.delay

/**
 * Global error display component that shows error messages as snackbars.
 */
@Composable
fun ErrorDisplay(
    snackbarHostState: SnackbarHostState,
    viewModel: ErrorViewModel = hiltViewModel()
) {
    val errorMessages by viewModel.errorMessages.collectAsState(initial = null)
    
    LaunchedEffect(errorMessages) {
        errorMessages?.let { errorMessage ->
            val result = snackbarHostState.showSnackbar(
                message = errorMessage.message,
                actionLabel = errorMessage.actionLabel,
                duration = if (errorMessage.actionLabel != null) {
                    SnackbarDuration.Indefinite
                } else {
                    SnackbarDuration.Long
                }
            )
            
            if (result == SnackbarResult.ActionPerformed) {
                errorMessage.onAction?.invoke()
            }
        }
    }
}

/**
 * Error dialog for critical errors that need user attention.
 */
@Composable
fun ErrorDialog(
    error: String?,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    title: String = "Error"
) {
    if (error != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                if (onRetry != null) {
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                } else {
                    Button(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            },
            dismissButton = if (onRetry != null) {
                {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            } else null
        )
    }
}

/**
 * Warning banner for non-critical issues.
 */
@Composable
fun WarningBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.warningContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onWarningContainer,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onWarningContainer,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onWarningContainer
                )
            }
        }
    }
}

/**
 * Loading state with error fallback.
 */
@Composable
fun LoadingWithError(
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(onClick = onRetry) {
                        Text("Try Again")
                    }
                }
            }
            
            else -> {
                content()
            }
        }
    }
}

// Extension to add warning container colors to ColorScheme
val ColorScheme.warningContainer: androidx.compose.ui.graphics.Color
    @Composable get() = if (this == MaterialTheme.colorScheme) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
    } else {
        this.errorContainer
    }

val ColorScheme.onWarningContainer: androidx.compose.ui.graphics.Color
    @Composable get() = if (this == MaterialTheme.colorScheme) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        this.onErrorContainer
    }