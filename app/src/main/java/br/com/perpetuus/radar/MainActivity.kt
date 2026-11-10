package br.com.perpetuus.radar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.perpetuus.radar.ui.RadarApp
import br.com.perpetuus.radar.ui.theme.RadarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as RadarApplication).repository
        setContent {
            RadarTheme {
                RadarApp(repository = repository)
            }
        }
    }
}
