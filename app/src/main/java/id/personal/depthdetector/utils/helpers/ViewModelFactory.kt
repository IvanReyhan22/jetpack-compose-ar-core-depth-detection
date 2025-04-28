package id.personal.depthdetector.utils.helpers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.personal.depthdetector.ui.features.screens.depth.repositories.ARRepository
import id.personal.depthdetector.ui.features.screens.depth.viewmodel.ARViewModel


class ViewModelFactory(private val context: Context) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ARViewModel::class.java)) {
            val repository = ARRepository(context)
            @Suppress("UNCHECKED_CAST")
            return ARViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}