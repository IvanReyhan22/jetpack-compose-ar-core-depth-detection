package id.personal.depthdetector.utils.helpers

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.personal.depthdetector.ui.features.screens.depth.viewmodel.DepthViewModel


class ViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DepthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DepthViewModel(application = application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}