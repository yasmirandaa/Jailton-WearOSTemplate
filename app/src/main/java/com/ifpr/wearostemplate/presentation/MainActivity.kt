/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.ifpr.wearostemplate.presentation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.wearostemplate.R
import com.ifpr.wearostemplate.presentation.baseclasses.Corrida
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.TextView






class MainActivity : ComponentActivity() {
    private var distanciaAtualKm = 0.0
    private var tempoAtualSegundos = 0L
    private var inicioCorrida = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContentView(R.layout.activity_main)
        val txtData = findViewById<TextView>(R.id.txtData)
        val txtHora = findViewById<TextView>(R.id.txtHora)

        val dataFormat = SimpleDateFormat("MMM dd", Locale("pt", "BR"))
        val horaFormat = SimpleDateFormat("HH:mm", Locale("pt", "BR"))

        txtData.text = dataFormat.format(Date()).uppercase()
        txtHora.text = horaFormat.format(Date())

        val btnEstatistica = findViewById<Button>(R.id.btnEstatistica)

        btnEstatistica.setOnClickListener{
            val intent = Intent(this, EstatisticaActivity::class.java)
            startActivity(intent)
        }

        val btnTreino = findViewById<Button>(R.id.btnTreino)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnStop.visibility = View.GONE

        btnTreino.setOnClickListener {
            inicioCorrida = System.currentTimeMillis()

            btnTreino.visibility = View.GONE
            btnStop.visibility = View.VISIBLE

            Toast.makeText(this, "Corrida iniciada", Toast.LENGTH_SHORT).show()
        }

        btnStop.setOnClickListener {
            tempoAtualSegundos = (System.currentTimeMillis() - inicioCorrida) / 1000

            salvarCorrida(distanciaAtualKm, tempoAtualSegundos)

            btnStop.visibility = View.GONE
            btnTreino.visibility = View.VISIBLE

            Toast.makeText(this, "Corrida finalizada", Toast.LENGTH_SHORT).show()
        }
    }
    private fun calcularRitmo(distanciaKm: Double, tempoSegundos:
    Long): String {
        if (distanciaKm <= 0.0) return "0:00"
        val segundosPorKm = (tempoSegundos / distanciaKm).toInt()
        val minutos = segundosPorKm / 60
        val segundos = segundosPorKm % 60
        return "$minutos:"+ segundos.toString().padStart(2, '0')
    }
    private fun salvarCorrida(distanciaKm: Double, tempoSegundos:
    Long) {
        val database = FirebaseDatabase.getInstance()
        val referencia = database.getReference("corridas")
        val id = referencia.push().key ?: return
        val data = SimpleDateFormat("dd/MM/yyyy HH:mm",
            Locale.getDefault()).format(Date())
        val ritmo = calcularRitmo(distanciaKm, tempoSegundos)
        val corrida = Corrida(distanciaKm, tempoSegundos, ritmo, data)
        referencia.child(id).setValue(corrida)
    }


}

