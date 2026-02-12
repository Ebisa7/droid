/*
 * Copyright 2024 LTQuiz Test
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ltquiz.test.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.ltquiz.test.ui.components.ErrorDisplay
import com.ltquiz.test.ui.navigation.XSendNavigation

/**
 * Main app composable with navigation and error handling for XSEND Meet.
 */
@Composable
fun LTQuizApp() {
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        XSendNavigation()
        
        // Global error display
        ErrorDisplay(snackbarHostState = snackbarHostState)
    }
}
