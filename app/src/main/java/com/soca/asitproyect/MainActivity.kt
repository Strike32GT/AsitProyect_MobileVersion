package com.soca.asitproyect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.soca.asitproyect.screens.MainIndex
import com.soca.asitproyect.ui.theme.ASITProyectTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(applicationContext, null, WellKnownTileServer.MapLibre)
        enableEdgeToEdge()
        setContent {
            MainIndex()
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ASITProyectTheme {
        Greeting("Android")
    }
}