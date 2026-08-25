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

    private lateinit var txtTempo: TextView
    private lateinit var txtDistancia: TextView
    private lateinit var txtRitmo: TextView

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnPerfil: Button

    // =========================================================
    // DADOS DA CORRIDA
    // =========================================================

    private var corridaEmAndamento = false

    private var tempoInicio: Long = 0L

    private var distanciaTotalMetros = 0.0

    private var ultimaLocalizacao: Location? = null

    // =========================================================
    // PERMISSÃO
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
    // TIMER
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

        txtTempo = findViewById(R.id.txtTempo)
        txtDistancia = findViewById(R.id.txtDistancia)
        txtRitmo = findViewById(R.id.txtRitmo)

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnPerfil = findViewById(R.id.btnPerfil)

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
    // BOTÕES
    // =========================================================

    private fun configurarBotoes() {

        btnPerfil.setOnClickListener {

            val intent =
                Intent(
                    this,
                    PerfilActivity::class.java
                )

            startActivity(intent)
        }

        btnStart.setOnClickListener {

            verificarPermissaoEIniciarCorrida()
        }

        btnStop.setOnClickListener {

            encerrarCorrida()
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

        distanciaTotalMetros = 0.0

        ultimaLocalizacao = null

        tempoInicio =
            SystemClock.elapsedRealtime()

        // -----------------------------------------------------
        // REINICIAR A INTERFACE
        // -----------------------------------------------------

        txtTempo.text = "00:00"

        txtDistancia.text = "0.00 km"

        txtRitmo.text = "-- min/km"

        // -----------------------------------------------------
        // INICIAR TIMER
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
    // CALLBACK DE LOCALIZAÇÃO
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
    // PROCESSAR NOVA LOCALIZAÇÃO
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
             * Evita registrar pequenas oscilações
             * causadas pela imprecisão do GPS.
             */
            if (deslocamentoMetros >= 2.0) {

                distanciaTotalMetros +=
                    deslocamentoMetros
            }
        }

        /*
         * A localização atual passa a ser
         * a referência da próxima medição.
         */
        ultimaLocalizacao =
            novaLocalizacao

        atualizarDadosNaTela()
    }

    // =========================================================
    // ATUALIZAR DISTÂNCIA E RITMO
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
                    ) / 1000

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
            tempoMilissegundos / 1000

        val minutos =
            segundosTotais / 60

        val segundos =
            segundosTotais % 60

        txtTempo.text =
            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutos,
                segundos
            )
    }

    // =========================================================
    // RITMO MÉDIO
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

        return tempoMinutos /
                distanciaKm
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

        return distanciaKm /
                tempoHoras
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
        // CALCULAR RESULTADOS
        // -----------------------------------------------------

        val tempoMilissegundos =
            SystemClock.elapsedRealtime() -
                    tempoInicio

        val tempoSegundos =
            tempoMilissegundos / 1000

        val distanciaKm =
            distanciaTotalMetros / 1000.0

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
    }

    // =========================================================
    // SALVAR CORRIDA
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
         * Peso temporário.
         *
         * Posteriormente poderá ser obtido
         * do perfil do usuário.
         */
        val pesoKg =
            70.0

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

    // =========================================================
    // PERMISSÃO
    // =========================================================

    private fun temPermissaoLocalizacao(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        fusedLocationClient.removeLocationUpdates(
            locationCallback
        )

        txtTempo.removeCallbacks(
            atualizadorTempo
        )

        super.onDestroy()
    }
}