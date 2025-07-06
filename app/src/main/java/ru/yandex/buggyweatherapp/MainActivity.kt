package ru.yandex.buggyweatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import ru.yandex.buggyweatherapp.ui.screens.WeatherScreen
import ru.yandex.buggyweatherapp.ui.theme.BuggyWeatherAppTheme
import ru.yandex.buggyweatherapp.viewmodel.WeatherViewModel

/*
- удалён неиспользуемый код
- форматирование
- добавлен hilt и замена ручного создания view mocel
- добавлены действия после запроса разрешений
 */

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // более безопасный способ без ручного создания
    private val weatherViewModel: WeatherViewModel by viewModels()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // отсутствие действие при получении резрашения
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                weatherViewModel.fetchCurrentLocationWeather()
            }

            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                weatherViewModel.fetchCurrentLocationWeather()
            }

            else -> {
                Toast.makeText(
                    this,
                    "Для работы нужны разрешения на местоположение",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkLocationPermissions()

        enableEdgeToEdge()

        setContent {
            BuggyWeatherAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WeatherScreen(
                        viewModel = weatherViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun checkLocationPermissions() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherAppPreview() {
    BuggyWeatherAppTheme {
        Text("Weather App Preview")
    }
}