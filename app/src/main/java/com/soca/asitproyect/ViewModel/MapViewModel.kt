package com.soca.asitproyect.ViewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
//import com.google.android.gms.maps.model.LatLng
import com.soca.asitproyect.data.model.AnalysisResult
import com.soca.asitproyect.data.model.GeoPoint
import kotlin.math.abs
import kotlin.math.max

class MapViewModel : ViewModel(){

    private val _selectedPoint = mutableStateOf<GeoPoint?>(null)
    val selectedPoint: State<GeoPoint?> = _selectedPoint

    private val _analysistResult = mutableStateOf<AnalysisResult?>(null)
    val analysistResult: State<AnalysisResult?> = _analysistResult

    private val _showInfoCard = mutableStateOf(false)
    val showInfoCard: State<Boolean> = _showInfoCard


    private val runwayElevation = 35.0
    private val slope = 0.02
    private val baseRadius = 3000.0

    fun onMapClick(point: GeoPoint) {
        _selectedPoint.value= point
        _analysistResult.value=null
    }


    fun onAnalysisFinished() {
        _showInfoCard.value = false
    }

    fun onObstacleTapped() {
        _showInfoCard.value = true
    }

    fun hideInfoCard() {
        _showInfoCard.value = false
    }


    fun analyze(elevation: Double){
        //Estos son valores de ejemplo

        val point = _selectedPoint.value ?: return
        val terrainElevation = simulatedTerrainElevation(point)
        val allowedElevation = runwayElevation + (baseRadius * slope)
        val excess = terrainElevation - allowedElevation

        _analysistResult.value = AnalysisResult(
            isAllowed = excess <= 0,
            metrosExcedentes = max(0.0, excess),
            radioPermitido = baseRadius
        )
        onAnalysisFinished()
    }


    private fun simulatedTerrainElevation(point: GeoPoint): Double {
        val base = 60.0
        val variation = abs( (point.latitud*1000).toInt() % 10 )
        return base + variation
    }
}