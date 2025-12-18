package com.soca.asitproyect.ViewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
//import com.google.android.gms.maps.model.LatLng
import com.soca.asitproyect.data.model.AnalysisResult
import com.soca.asitproyect.data.model.GeoPoint

class MapViewModel : ViewModel(){

    private val _selectedPoint = mutableStateOf<GeoPoint?>(null)
    val selectedPoint: State<GeoPoint?> = _selectedPoint

    private val _analysistResult = mutableStateOf<AnalysisResult?>(null)
    val analysistResult: State<AnalysisResult?> = _analysistResult

    fun onMapClick(point: GeoPoint) {
        _selectedPoint.value= point
        _analysistResult.value=null
    }

    fun analyze(elevation: Double){
        //Estos son valores de ejemplo
        val MAX_ELEVATION = 50.0
        val ALLOWED_RADIUS = 3000.0

        val exceso = elevation - MAX_ELEVATION

        _analysistResult.value = AnalysisResult(
            isAllowed = exceso <=0,
            metrosExcedentes = if (exceso > 0 ) exceso else 0.0,
            radioPermitido = ALLOWED_RADIUS
        )
    }
}