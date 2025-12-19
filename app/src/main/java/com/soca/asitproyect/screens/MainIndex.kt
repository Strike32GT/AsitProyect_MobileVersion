package com.soca.asitproyect.screens

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.*
import org.maplibre.android.style.layers.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.*
import org.maplibre.turf.TurfJoins
import com.soca.asitproyect.ViewModel.MapViewModel
import com.soca.asitproyect.data.model.AnalysisResult
import com.soca.asitproyect.data.model.GeoPoint
import kotlin.math.*

private const val STYLE_URL = "https://demotiles.maplibre.org/style.json"

private const val SRC_ALLOWED = "src_allowed"
private const val SRC_EXCEED = "src_exceed"
private const val LAYER_ALLOWED_FILL = "layer_allowed_fill"
private const val LAYER_ALLOWED_LINE = "layer_allowed_line"
private const val LAYER_EXCEED_FILL = "layer_exceed_fill"
private const val LAYER_EXCEED_LINE = "layer_exceed_line"

@Composable
fun MainIndex() {
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: MapViewModel = viewModel()
    val puntoSeleccion by viewModel.selectedPoint
    val resultadoAnalisis by viewModel.analysistResult
    val showInfoCard by viewModel.showInfoCard

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapStyle by remember { mutableStateOf<Style?>(null) }
    var realMapView: MapView? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).also { mv ->
                    mv.onCreate(Bundle())
                    realMapView = mv

                    mv.getMapAsync { map ->
                        mapLibreMap = map

                        map.uiSettings.apply {
                            isZoomGesturesEnabled = true
                            isScrollGesturesEnabled = true
                            isRotateGesturesEnabled = true
                            isTiltGesturesEnabled = true
                            isDoubleTapGesturesEnabled = true
                        }

                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(-12.0464, -77.0428), 12.5
                            )
                        )

                        map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                            mapStyle = style

                            if (style.getSource(SRC_ALLOWED) == null) {
                                style.addSource(
                                    GeoJsonSource(SRC_ALLOWED, FeatureCollection.fromFeatures(arrayOf()))
                                )
                            }
                            if (style.getSource(SRC_EXCEED) == null) {
                                style.addSource(
                                    GeoJsonSource(SRC_EXCEED, FeatureCollection.fromFeatures(arrayOf()))
                                )
                            }

                            if (style.getLayer(LAYER_ALLOWED_FILL) == null) {
                                style.addLayer(
                                    FillLayer(LAYER_ALLOWED_FILL, SRC_ALLOWED).withProperties(
                                        PropertyFactory.fillColor("rgba(255,0,0,0.35)")
                                    )
                                )
                            }
                            if (style.getLayer(LAYER_ALLOWED_LINE) == null) {
                                style.addLayer(
                                    LineLayer(LAYER_ALLOWED_LINE, SRC_ALLOWED).withProperties(
                                        PropertyFactory.lineColor("rgba(255,0,0,0.8)"),
                                        PropertyFactory.lineWidth(2f)
                                    )
                                )
                            }

                            if (style.getLayer(LAYER_EXCEED_FILL) == null) {
                                style.addLayer(
                                    FillLayer(LAYER_EXCEED_FILL, SRC_EXCEED).withProperties(
                                        PropertyFactory.fillColor("rgba(0,0,255,0.25)")
                                    )
                                )
                            }
                            if (style.getLayer(LAYER_EXCEED_LINE) == null) {
                                style.addLayer(
                                    LineLayer(LAYER_EXCEED_LINE, SRC_EXCEED).withProperties(
                                        PropertyFactory.lineColor("rgba(0,0,255,0.9)"),
                                        PropertyFactory.lineWidth(2f)
                                    )
                                )
                            }

                            map.addOnMapClickListener { latLng ->
                                val point = GeoPoint(latLng.latitude, latLng.longitude)
                                val style = mapStyle
                                val result = resultadoAnalisis
                                if(style !=null && result !=null){
                                    val allowedSource = style.getSourceAs<GeoJsonSource>(SRC_ALLOWED)
                                    val collection = allowedSource?.querySourceFeatures(null)

                                    val polygon = collection
                                        ?.firstOrNull()
                                        ?.geometry() as? Polygon

                                    if (polygon !=null && isPointInsidePolygon(point.latitud, point.longitud, polygon)){
                                        viewModel.onObstacleTapped()
                                        return@addOnMapClickListener true
                                    }
                                }
                                viewModel.onMapClick(point)
                                true
                            }
                        }
                    }
                }
            }
        )

        DisposableEffect(lifecycleOwner, realMapView) {
            val mv = realMapView
            val observer = LifecycleEventObserver { _, event ->
                if (mv == null) return@LifecycleEventObserver
                when (event) {
                    Lifecycle.Event.ON_START -> mv.onStart()
                    Lifecycle.Event.ON_RESUME -> mv.onResume()
                    Lifecycle.Event.ON_PAUSE -> mv.onPause()
                    Lifecycle.Event.ON_STOP -> mv.onStop()
                    Lifecycle.Event.ON_DESTROY -> mv.onDestroy()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(mapStyle, puntoSeleccion, resultadoAnalisis) {
            val style = mapStyle ?: return@LaunchedEffect

            val allowedRadius = resultadoAnalisis?.radioPermitido
            val exceedExtra = if (resultadoAnalisis?.isAllowed == false) resultadoAnalisis?.metrosExcedentes else null

            val allowedPolygon: Polygon? =
                if (puntoSeleccion != null && allowedRadius != null) {
                    withContext(Dispatchers.Default) {
                        circlePolygon(
                            lat = puntoSeleccion!!.latitud,
                            lng = puntoSeleccion!!.longitud,
                            radiusMeters = allowedRadius,
                            points = 96
                        )
                    }
                } else null

            val exceedPolygon: Polygon? =
                if (puntoSeleccion != null && allowedRadius != null && exceedExtra != null && exceedExtra > 0.0) {
                    withContext(Dispatchers.Default) {
                        circlePolygon(
                            lat = puntoSeleccion!!.latitud,
                            lng = puntoSeleccion!!.longitud,
                            radiusMeters = allowedRadius + exceedExtra,
                            points = 96
                        )
                    }
                } else null

            updateAnalysisLayersWithPreparedPolygons(
                style = style,
                allowedPolygon = allowedPolygon,
                exceedPolygon = exceedPolygon
            )
        }

        Button(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            enabled = puntoSeleccion != null,
            onClick = { viewModel.analyze(elevation = 80.0) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF014680)
            )
        ) {
            Text(text = "Analizar")
        }



        

        resultadoAnalisis?.let { result ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.isAllowed) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    text = if (result.isAllowed)
                        "Obra permitida"
                    else
                        "Excede ${result.metrosExcedentes.toInt()} metros"
                )
            }
        }

        if(showInfoCard && puntoSeleccion !=null && resultadoAnalisis != null){
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) {
                ObstacleInfoCard(
                    point = puntoSeleccion!!,
                    result = resultadoAnalisis!!,
                    onClose = {viewModel.hideInfoCard()}
                )
            }
        }
    }
}

private fun updateAnalysisLayersWithPreparedPolygons(
    style: Style,
    allowedPolygon: Polygon?,
    exceedPolygon: Polygon?
) {
    val allowed = style.getSourceAs<GeoJsonSource>(SRC_ALLOWED)
    val exceed = style.getSourceAs<GeoJsonSource>(SRC_EXCEED)

    if (allowedPolygon == null) {
        allowed?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
        exceed?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
        return
    }

    allowed?.setGeoJson(
        FeatureCollection.fromFeatures(arrayOf(Feature.fromGeometry(allowedPolygon)))
    )

    if (exceedPolygon != null) {
        exceed?.setGeoJson(
            FeatureCollection.fromFeatures(arrayOf(Feature.fromGeometry(exceedPolygon)))
        )
    } else {
        exceed?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
    }
}


private fun isPointInsidePolygon(
    lat: Double,
    lng: Double,
    polygon: Polygon
): Boolean {
    val point = Point.fromLngLat(lng, lat)
    return TurfJoins.inside(point, polygon)
}


@Composable
fun ObstacleInfoCard(point: GeoPoint, result: AnalysisResult, onClose: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ){
        Column(
            modifier = Modifier.padding(16.dp)
        ){
            Text(
                text = "Información del obstáculo",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Latitud: ${"%.6f".format(point.latitud)}")
            Text("Longitud: ${"%.6f".format(point.longitud)}")
            Text("Radio Permitido: ${result.radioPermitido.toInt()} m")

            if(!result.isAllowed){
                Text(
                    text = "Exceso: ${result.metrosExcedentes.toInt()} m",
                    color = Color.Red
                )
            } else {
                Text(
                    text= "Dentro del limite permitido",
                    color= Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF014680))
            ){
                Text(
                    text = "Cerrar",
                    color = Color.White
                )
            }
        }
    }
}


private fun circlePolygon(
    lat: Double,
    lng: Double,
    radiusMeters: Double,
    points: Int = 96
): Polygon {
    val earth = 6371000.0
    val latRad = Math.toRadians(lat)
    val lngRad = Math.toRadians(lng)
    val angDist = radiusMeters / earth

    val coords = mutableListOf<Point>()
    repeat(points) { i ->
        val bearing = 2.0 * Math.PI * i.toDouble() / points.toDouble()

        val lat2 = asin(
            sin(latRad) * cos(angDist) + cos(latRad) * sin(angDist) * cos(bearing)
        )
        val lng2 = lngRad + atan2(
            sin(bearing) * sin(angDist) * cos(latRad),
            cos(angDist) - sin(latRad) * sin(lat2)
        )

        coords.add(Point.fromLngLat(Math.toDegrees(lng2), Math.toDegrees(lat2)))
    }

    coords.add(coords.first())
    return Polygon.fromLngLats(listOf(coords))
}