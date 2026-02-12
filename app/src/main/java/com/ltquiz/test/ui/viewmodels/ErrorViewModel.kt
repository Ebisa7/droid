package com.ltquiz.test.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.ltquiz.test.errors.ErrorHandler
import com.ltquiz.test.errors.ErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@HiltViewModel
class ErrorViewModel @Inject constructor(
    private val errorHandler: ErrorHandler
) : ViewModel() {
    
    val errorMessages: SharedFlow<ErrorMessage> = errorHandler.errorMessages
}