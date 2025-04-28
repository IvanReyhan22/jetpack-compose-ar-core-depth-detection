package id.personal.depthdetector.ui.features.screens.depth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Frame
import id.personal.depthdetector.ui.features.screens.depth.repositories.ARRepository
import id.personal.depthdetector.ui.features.screens.depth.states.DistanceMeasurement
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ARViewModel(private val repository: ARRepository) : ViewModel() {
    val distanceMeasurement: StateFlow<DistanceMeasurement?> = repository.distanceMeasurement
    val isArCoreAvailable: StateFlow<Boolean> = repository.isArCoreAvailable

    init {
        viewModelScope.launch {
            repository.distanceMeasurement.collect { measurement ->
                // Additional processing if needed
            }
        }
    }

    fun initializeArSession() {
        repository.initializeArSession()
    }

    fun pauseArSession() {
        repository.pauseArSession()
    }

    fun resumeArSession() {
        repository.resumeArSession()
    }

    fun updateFrame(frame: Frame?) {
        repository.updateFrame(frame)
    }
}