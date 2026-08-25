package com.ifpr.wearostemplate.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    // =========================================================
    // LOCALIZAÇÃO
    // =========================================================

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

    // =========================================================
    // COMPONENTES DA INTERFACE
    // =========================================================

    private lateinit var txtData: TextView
    private lateinit var txtHora: TextView

    private lateinit var txtTempo: TextView
    private lateinit var txtDistancia: TextView
    private lateinit var txtRitmo: TextView

    private lateinit var btnTreino: Button
    private lateinit var btnStop: Button
    private lateinit var btnEstatistica: Button

    // =========================================================
    // DADOS DA CORRIDA
    // =========================================================

    private var corridaEmAndamento = false

    private var tempoInicio = 0L

    private var distanciaTotalMetros = 0.0

    private var ultimaLocalizacao: Location? = null

    // =========================================================
    // PERMISSÃO DE LOCALIZAÇÃO
    // =========================================================

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

    // =========================================================
    // CRONÔMETRO
    // =========================================================

    private val atualizadorTempo = object : Runnable {

        override fun run() {

            if (!corridaEmAndamento) {
                return
            }

            val tempoDecorrido =
                SystemClock.elapsedRealtime() - tempoInicio

            atualizarTempoNaTela(tempoDecorrido)

            txtTempo.postDelayed(
                this,
                1000L
            )
        }
    }

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContentView(R.layout.activity_main)

        // -----------------------------------------------------
        // COMPONENTES
        // -----------------------------------------------------

        txtData = findViewById(R.id.txtData)
        txtHora = findViewById(R.id.txtHora)

        btnTreino = findViewById(R.id.btnTreino)
        btnStop = findViewById(R.id.btnStop)
        btnEstatistica = findViewById(R.id.btnEstatistica)

        // -----------------------------------------------------
        // DATA E HORA
        // -----------------------------------------------------

        atualizarDataHora()

        // -----------------------------------------------------
        // ESTADO INICIAL
        // -----------------------------------------------------

        txtTempo.text = "00:00"
        txtDistancia.text = "0.00 km"
        txtRitmo.text = "-- min/km"

        btnTreino.visibility = android.view.View.VISIBLE
        btnStop.visibility = android.view.View.GONE

        // -----------------------------------------------------
        // LOCALIZAÇÃO
        // -----------------------------------------------------

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        configurarLocationCallback()

        // -----------------------------------------------------
        // BOTÕES
        // -----------------------------------------------------

        configurarBotoes()
    }

    // =========================================================
    // DATA E HORA
    // =========================================================

    private fun atualizarDataHora() {

        val agora = Date()

        val dataFormat =
            SimpleDateFormat(
                "MMM dd",
                Locale("pt", "BR")
            )

        val horaFormat =
            SimpleDateFormat(
                "HH:mm",
                Locale("pt", "BR")
            )

        txtData.text =
            dataFormat
                .format(agora)
                .uppercase()

        txtHora.text =
            horaFormat.format(agora)
    }

    // =========================================================
    // BOTÕES
    // =========================================================

    private fun configurarBotoes() {

        // -----------------------------------------------------
        // BOTÃO INICIAR
        // -----------------------------------------------------

        btnTreino.setOnClickListener {

            verificarPermissaoEIniciarCorrida()
        }

        // -----------------------------------------------------
        // BOTÃO STOP
        // -----------------------------------------------------

        btnStop.setOnClickListener {

            encerrarCorrida()
        }

        // -----------------------------------------------------
        // BOTÃO STATUS
        // -----------------------------------------------------

        btnEstatistica.setOnClickListener {

            val intent =
                Intent(
                    this,
                    EstatisticaActivity::class.java
                )

            startActivity(intent)
        }
    }

    // =========================================================
    // VERIFICAR PERMISSÃO
    // =========================================================

    private fun verificarPermissaoEIniciarCorrida() {

        if (temPermissaoLocalizacao()) {

            iniciarCorrida()

        } else {

            solicitarPermissaoLocalizacao.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    // =========================================================
    // INICIAR CORRIDA
    // =========================================================

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

        // -----------------------------------------------------
        // RESETAR DADOS
        // -----------------------------------------------------

        distanciaTotalMetros = 0.0

        ultimaLocalizacao = null

        tempoInicio =
            SystemClock.elapsedRealtime()

        // -----------------------------------------------------
        // ATUALIZAR INTERFACE
        // -----------------------------------------------------

        txtTempo.text = "00:00"

        txtDistancia.text = "0.00 km"

        txtRitmo.text = "-- min/km"

        btnTreino.visibility =
            android.view.View.GONE

        btnStop.visibility =
            android.view.View.VISIBLE

        // -----------------------------------------------------
        // INICIAR CRONÔMETRO
        // -----------------------------------------------------

        txtTempo.post(
            atualizadorTempo
        )

        // -----------------------------------------------------
        // INICIAR GPS
        // -----------------------------------------------------

        iniciarAtualizacoesLocalizacao()

        Toast.makeText(
            this,
            "Corrida iniciada!",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // CONFIGURAR CALLBACK DO GPS
    // =========================================================

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

    // =========================================================
    // INICIAR ATUALIZAÇÕES DE GPS
    // =========================================================

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

    // =========================================================
    // PROCESSAR LOCALIZAÇÃO
    // =========================================================

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

            /*
             * Ignora pequenas oscilações do GPS.
             */
            if (deslocamentoMetros >= 2.0) {

                distanciaTotalMetros +=
                    deslocamentoMetros
            }
        }

        ultimaLocalizacao =
            novaLocalizacao

        atualizarDadosNaTela()
    }

    // =========================================================
    // ATUALIZAR DADOS
    // =========================================================

    private fun atualizarDadosNaTela() {

        val distanciaKm =
            distanciaTotalMetros / 1000.0

        txtDistancia.text =
            String.format(
                Locale.getDefault(),
                "%.2f km",
                distanciaKm
            )

        val tempoSegundos =
            (
                    SystemClock.elapsedRealtime() -
                            tempoInicio
                    ) / 1000L

        if (
            distanciaKm > 0.0 &&
            tempoSegundos > 0
        ) {

            val ritmo =
                calcularRitmoMedio(
                    tempoSegundos,
                    distanciaKm
                )

            txtRitmo.text =
                formatarRitmo(
                    ritmo
                )
        }
    }

    // =========================================================
    // ATUALIZAR CRONÔMETRO
    // =========================================================

    private fun atualizarTempoNaTela(
        tempoMilissegundos: Long
    ) {

        val segundosTotais =
            tempoMilissegundos / 1000L

        val minutos =
            segundosTotais / 60L

        val segundos =
            segundosTotais % 60L

        txtTempo.text =
            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutos,
                segundos
            )
    }

    // =========================================================
    // CALCULAR RITMO
    // =========================================================

    private fun calcularRitmoMedio(
        tempoSegundos: Long,
        distanciaKm: Double
    ): Double {

        if (distanciaKm <= 0.0) {
            return 0.0
        }

        val tempoMinutos =
            tempoSegundos / 60.0

        return tempoMinutos / distanciaKm
    }

    // =========================================================
    // FORMATAR RITMO
    // =========================================================

    private fun formatarRitmo(
        ritmo: Double
    ): String {

        if (ritmo <= 0.0) {
            return "-- min/km"
        }

        val minutos =
            ritmo.toInt()

        val segundos =
            ((ritmo - minutos) * 60)
                .toInt()

        return String.format(
            Locale.getDefault(),
            "%d:%02d min/km",
            minutos,
            segundos
        )
    }

    // =========================================================
    // VELOCIDADE MÉDIA
    // =========================================================

    private fun calcularVelocidadeMedia(
        distanciaKm: Double,
        tempoSegundos: Long
    ): Double {

        if (tempoSegundos <= 0L) {
            return 0.0
        }

        val tempoHoras =
            tempoSegundos / 3600.0

        return distanciaKm / tempoHoras
    }

    // =========================================================
    // CALORIAS
    // =========================================================

    private fun calcularCalorias(
        distanciaKm: Double,
        pesoKg: Double
    ): Double {

        return distanciaKm *
                pesoKg *
                1.036
    }

    // =========================================================
    // ENCERRAR CORRIDA
    // =========================================================

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

        // -----------------------------------------------------
        // PARAR GPS
        // -----------------------------------------------------

        fusedLocationClient.removeLocationUpdates(
            locationCallback
        )

        // -----------------------------------------------------
        // PARAR CRONÔMETRO
        // -----------------------------------------------------

        txtTempo.removeCallbacks(
            atualizadorTempo
        )

        // -----------------------------------------------------
        // CALCULAR TEMPO
        // -----------------------------------------------------

        val tempoMilissegundos =
            SystemClock.elapsedRealtime() -
                    tempoInicio

        val tempoSegundos =
            tempoMilissegundos / 1000L

        val distanciaKm =
            distanciaTotalMetros / 1000.0

        // -----------------------------------------------------
        // ATUALIZAR BOTÕES
        // -----------------------------------------------------

        btnStop.visibility =
            android.view.View.GONE

        btnTreino.visibility =
            android.view.View.VISIBLE

        // -----------------------------------------------------
        // VERIFICAR DISTÂNCIA
        // -----------------------------------------------------

        if (distanciaKm <= 0.0) {

            Toast.makeText(
                this,
                "Nenhuma distância foi registrada.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // -----------------------------------------------------
        // SALVAR
        // -----------------------------------------------------

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

    // =========================================================
    // SALVAR NO FIREBASE
    // =========================================================

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

        /*
         * Peso provisório.
         *
         * Depois podemos buscar o peso
         * cadastrado no PerfilActivity/Firebase.
         */
        val pesoKg = 70.0

        val calorias =
            calcularCalorias(
                distanciaKm,
                pesoKg
            )

        // -----------------------------------------------------
        // FIREBASE
        // -----------------------------------------------------

        val referencia =
            FirebaseDatabase
                .getInstance()
                .getReference("corridas")

        val id =
            referencia
                .push()
                .key
                ?: return

        val data =
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            ).format(Date())

        val corrida =
            Corrida(
                distanciaKm,
                tempoSegundos,
                formatarRitmo(ritmoMedio),
                data
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

    // =========================================================
    // VERIFICAR PERMISSÃO
    // =========================================================

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

    // =========================================================
    // ON DESTROY
    // =========================================================

    override fun onDestroy() {

        if (::fusedLocationClient.isInitialized &&
            ::locationCallback.isInitialized
        ) {

            fusedLocationClient.removeLocationUpdates(
                locationCallback
            )
        }

        if (::txtTempo.isInitialized) {

            txtTempo.removeCallbacks(
                atualizadorTempo
            )
        }

        super.onDestroy()
    }
}
