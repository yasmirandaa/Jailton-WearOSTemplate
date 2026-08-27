package com.ifpr.wearostemplate.presentation

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.ifpr.wearostemplate.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EstatisticaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setTheme(
            android.R.style.Theme_DeviceDefault
        )

        setContentView(
            R.layout.activity_estatistica
        )

        val txtData =
            findViewById<TextView>(
                R.id.txtData
            )

        val txtHora =
            findViewById<TextView>(
                R.id.txtHora
            )

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
                .format(Date())
                .uppercase()

        txtHora.text =
            horaFormat.format(Date())

        val btnVoltar =
            findViewById<Button>(
                R.id.btnVoltar
            )

        btnVoltar.setOnClickListener {
            finish()
        }
    }
}