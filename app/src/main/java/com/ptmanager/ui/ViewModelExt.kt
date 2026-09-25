package com.ptmanager.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ptmanager.AppContainer
import com.ptmanager.PTManagerApp

@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (AppContainer) -> VM,
): VM = viewModel(factory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as Application
        create((app as PTManagerApp).container)
    }
})