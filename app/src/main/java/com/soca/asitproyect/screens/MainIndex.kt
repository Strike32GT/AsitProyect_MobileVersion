package com.soca.asitproyect.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.soca.asitproyect.ViewModel.MapViewModel

@Composable
fun MainIndex() {

    val viewModel: MapViewModel = viewModel()
    val puntoSeleccion by viewModel.selectedPoint
    val resultadoAnalisis by viewModel.analysistResult

    Box(modifier = Modifier
        .fillMaxSize()
    ){
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            onMapClick = { viewModel.onMapClick(it)}
        ){
            resultadoAnalisis?.let { result ->
                puntoSeleccion?.let { center ->

                    Circle(
                        center = center,
                        radius = result.radioPermitido,
                        fillColor = Color.Red.copy(alpha = 0.35f),
                        strokeColor = Color.Red
                    )

                    if(!result.isAllowed){
                        Circle(
                            center = center,
                            radius = result.radioPermitido + result.metrosExcedentes,
                            fillColor = Color.Blue.copy(alpha = 0.25f),
                            strokeColor = Color.Blue
                        )
                    }
                }
            }
        }

        Button(modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
            enabled = puntoSeleccion != null,
            onClick = {
                viewModel.analyze(elevation = 80.0)
            }
        ) {
            Text(
                text = "Analizar"
            )
        }

            resultadoAnalisis?.let { result ->
                Card(modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                ){
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = if(result.isAllowed){
                            "Obra Permitida"
                        }
                        else {
                            "Excede ${result.metrosExcedentes.toInt()} metros"
                        }
                    )
                }
            }
    }
}