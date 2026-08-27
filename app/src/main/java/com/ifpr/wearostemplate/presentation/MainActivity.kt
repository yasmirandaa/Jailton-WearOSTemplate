package com.ifpr.wearostemplate.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.wearostemplate.R
import com.ifpr.wearostemplate.presentation.baseclasses.Corrida

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val locationRequest =
        LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        )
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(2f)
            .build()

    private lateinit var btnTreino: Button
    private lateinit var btnStop: Button
    private lateinit var btnEstatistica: Button

    private var corridaEmAndamento = false
    private var tempoInicio: Long = 0L
    private var distanciaTotalMetros = 0.0
    private var ultimaLocalizacao: Location? = null

    private val solicitarPermissaoLocalizacao =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { permitido ->

            if (permitido) {
                iniciarCorrida()
            } else {
                Toast.makeText(
                    this,
                    "A localização é necessária para registrar a corrida.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContentView(R.layout.activity_main)

        btnTreino = findViewById(R.id.btnTreino)
        btnStop = findViewById(R.id.btnStop)
        btnEstatistica = findViewById(R.id.btnEstatistica)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        configurarLocationCallback()
        configurarBotoes()
    }

    private fun configurarBotoes() {

        btnTreino.setOnClickListener {
            verificarPermissaoEIniciarCorrida()
        }

        btnStop.setOnClickListener {
            encerrarCorrida()
        }

        btnEstatistica.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    EstatisticaActivity::class.java
                )
            )
        }
    }

    private fun verificarPermissaoEIniciarCorrida() {

        if (temPermissaoLocalizacao()) {
            iniciarCorrida()
        } else {
            solicitarPermissaoLocalizacao.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun iniciarCorrida() {

        if (corridaEmAndamento) {

            Toast.makeText(
                this,
                "A corrida já foi iniciada.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (!temPermissaoLocalizacao()) {
            return
        }

        corridaEmAndamento = true
        distanciaTotalMetros = 0.0
        ultimaLocalizacao = null

        tempoInicio =
            SystemClock.elapsedRealtime()

        btnTreino.visibility = View.GONE
        btnStop.visibility = View.VISIBLE

        iniciarAtualizacoesLocalizacao()

        Toast.makeText(
            this,
            "Corrida iniciada!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun configurarLocationCallback() {

        locationCallback =
            object : LocationCallback() {

                override fun onLocationResult(
                    locationResult: LocationResult
                ) {

                    if (!corridaEmAndamento) {
                        return
                    }

                    for (localizacao in locationResult.locations) {
                        processarNovaLocalizacao(
                            localizacao
                        )
                    }
                }
            }
    }

    private fun iniciarAtualizacoesLocalizacao() {

        val permissaoFine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val permissaoCoarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!permissaoFine && !permissaoCoarse) {
            return
        }

        try {

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )

        } catch (e: SecurityException) {

            Toast.makeText(
                this,
                "Não foi possível acessar a localização.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun processarNovaLocalizacao(
        novaLocalizacao: Location
    ) {

        val anterior =
            ultimaLocalizacao

        if (anterior != null) {

            val deslocamentoMetros =
                anterior.distanceTo(
                    novaLocalizacao
                )

            if (deslocamentoMetros >= 2.0) {

                distanciaTotalMetros +=
                    deslocamentoMetros
            }
        }

        ultimaLocalizacao =
            novaLocalizacao
    }

    private fun encerrarCorrida() {

        if (!corridaEmAndamento) {

            Toast.makeText(
                this,
                "Nenhuma corrida em andamento.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        corridaEmAndamento = false

        fusedLocationClient.removeLocationUpdates(
            locationCallback
        )

        val tempoMilissegundos =
            SystemClock.elapsedRealtime() -
                    tempoInicio

        val tempoSegundos =
            tempoMilissegundos / 1000L

        val distanciaKm =
            distanciaTotalMetros / 1000.0

        btnStop.visibility =
            View.GONE

        btnTreino.visibility =
            View.VISIBLE

        if (distanciaKm <= 0.0) {

            Toast.makeText(
                this,
                "Nenhuma distância foi registrada.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        salvarCorrida(
            distanciaKm,
            tempoSegundos
        )

        Toast.makeText(
            this,
            "Corrida finalizada!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun calcularRitmoMedio(
        tempoSegundos: Long,
        distanciaKm: Double
    ): Double {

        if (distanciaKm <= 0.0) {
            return 0.0
        }

        val tempoMinutos =
            tempoSegundos / 60.0

        return tempoMinutos /
                distanciaKm
    }

    private fun calcularVelocidadeMedia(
        distanciaKm: Double,
        tempoSegundos: Long
    ): Double {

        if (tempoSegundos <= 0L) {
            return 0.0
        }

        val tempoHoras =
            tempoSegundos / 3600.0

        return distanciaKm /
                tempoHoras
    }

    private fun calcularCalorias(
        distanciaKm: Double,
        pesoKg: Double
    ): Double {

        return distanciaKm *
                pesoKg *
                1.036
    }

    private fun salvarCorrida(
        distanciaKm: Double,
        tempoSegundos: Long
    ) {

        val ritmoMedio =
            calcularRitmoMedio(
                tempoSegundos,
                distanciaKm
            )

        val velocidadeMedia =
            calcularVelocidadeMedia(
                distanciaKm,
                tempoSegundos
            )

        val pesoKg = 70.0

        val calorias =
            calcularCalorias(
                distanciaKm,
                pesoKg
            )

        val referencia =
            FirebaseDatabase
                .getInstance()
                .getReference("corridas")

        val id =
            referencia
                .push()
                .key
                ?: return

        val corrida =
            Corrida(
                id = id,
                distanciaKm = distanciaKm,
                tempoSegundos = tempoSegundos,
                ritmoMedio = ritmoMedio,
                velocidadeMedia = velocidadeMedia,
                calorias = calorias,
                data = System.currentTimeMillis()
            )

        referencia
            .child(id)
            .setValue(corrida)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Corrida salva com sucesso!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Erro ao salvar a corrida.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun temPermissaoLocalizacao(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {

        if (::fusedLocationClient.isInitialized &&
            ::locationCallback.isInitialized
        ) {

            fusedLocationClient.removeLocationUpdates(
                locationCallback
            )
        }

        super.onDestroy()
    }
}