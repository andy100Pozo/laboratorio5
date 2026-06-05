package com.example.demodata.ui.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.demodata.data.repository.GpsRepository

class GpsViewModelFactory(
    private val repository: GpsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GpsViewModel::class.java)) {
            return GpsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}