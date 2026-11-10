package br.com.perpetuus.radar

import android.app.Application
import br.com.perpetuus.radar.data.RadarDatabase
import br.com.perpetuus.radar.data.RadarRepository

class RadarApplication : Application() {
    val database by lazy { RadarDatabase.get(this) }
    val repository by lazy { RadarRepository(this, database) }
}
