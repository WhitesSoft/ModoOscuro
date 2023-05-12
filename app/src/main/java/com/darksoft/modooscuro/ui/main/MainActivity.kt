package com.darksoft.modooscuro.ui.main

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darksoft.modooscuro.databinding.ActivityMainBinding
import com.darksoft.modooscuro.ui.main.model.SettingsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/* DATASTORE
*  Unica instancia de la clase DataStore para toda la app
* */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

class MainActivity : AppCompatActivity() {

    // Constantes
    companion object {
        const val MODE_DARK = "mode_dark"
        const val ENABLED_BLUETOOTH = "enabled_bluetooth"
        const val ENABLED_VIBRATION_PHONE = "enabled_vibration_phone"
        const val VOLUME_LEVEL = "volume_level"
    }

    private lateinit var binding: ActivityMainBinding
    private var firstTime = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nos enganchamos al flow de dataStore
        CoroutineScope(Dispatchers.IO).launch {
            getSettings().filter { firstTime }.collect{ settingsModel ->
                // Ejecutamos en el hilo principal
                runOnUiThread{
                    binding.switchVibration.isChecked = settingsModel.enabledVibrationPhone
                    binding.switchBluetooth.isChecked = settingsModel.enabledBluetooth
                    binding.switchModeDark.isChecked = settingsModel.modeDark
                    binding.rangeSlider.setValues(settingsModel.volumeLevel.toFloat())
                    firstTime = !firstTime
                }
            }
        }

        initUI()
    }

    private fun initUI() {

        binding.rangeSlider.addOnChangeListener { _, value, _ ->
            // Guardar valor en DataStore
            CoroutineScope(Dispatchers.IO).launch {
                saveVolume(value.toInt())
            }
        }

        binding.switchModeDark.setOnCheckedChangeListener { _, isChecked ->
            // Guardar valor en DataStore
            CoroutineScope(Dispatchers.IO).launch {
                saveOptions(MODE_DARK, isChecked)
            }
        }

        binding.switchBluetooth.setOnCheckedChangeListener { _, isChecked ->
            // Guardar valor en DataStore
            CoroutineScope(Dispatchers.IO).launch {
                saveOptions(ENABLED_BLUETOOTH, isChecked)
            }
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            // Guardar valor en DataStore
            CoroutineScope(Dispatchers.IO).launch {
                saveOptions(ENABLED_VIBRATION_PHONE, isChecked)
            }
        }

    }

    // los valores de dataStore son asincronos asi que se debe usar corutinas
    private suspend fun saveVolume(value: Int){
        dataStore.edit { settings ->
            settings[intPreferencesKey(VOLUME_LEVEL)] = value
        }
    }

    private suspend fun saveOptions(key: String, value: Boolean){
        dataStore.edit { settings ->
            settings[booleanPreferencesKey(key)] = value
        }
    }

    // Obtenemos los valores de dataStore
    private fun getSettings(): Flow<SettingsModel>  =
        dataStore.data.map { preferences -> SettingsModel(
            modeDark = preferences[booleanPreferencesKey(MODE_DARK)] ?: false,
            enabledBluetooth = preferences[booleanPreferencesKey(ENABLED_BLUETOOTH)] ?: false,
            enabledVibrationPhone = preferences[booleanPreferencesKey(ENABLED_VIBRATION_PHONE)] ?: false,
            volumeLevel = preferences[intPreferencesKey(VOLUME_LEVEL)] ?: 0
        ) }

}