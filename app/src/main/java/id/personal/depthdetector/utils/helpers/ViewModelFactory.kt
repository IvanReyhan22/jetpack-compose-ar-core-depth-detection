package id.personal.depthdetector.utils.helpers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.personal.depthdetector.ui.features.screens.depth.viewmodel.ARViewModel


class ViewModelFactory(private val context: Context) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ARViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ARViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}