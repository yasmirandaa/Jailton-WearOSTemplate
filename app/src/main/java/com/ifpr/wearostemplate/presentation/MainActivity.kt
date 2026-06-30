/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.ifpr.wearostemplate.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ifpr.wearostemplate.R
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
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
    }
}

