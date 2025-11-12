package com.example.poketeambuilder

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.poketeambuilder.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

    // Aplicar insets del sistema al padding de la vista raíz
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    // Añadir o recuperar dinámicamente el HomeFragment en el contenedor
        val containerId = R.id.fragment_container
        val existing = supportFragmentManager.findFragmentById(containerId)
        if (existing == null) {
            val home = HomeFragment()
            supportFragmentManager.beginTransaction()
                .replace(containerId, home, "HOME_FRAGMENT")
                .commit()
        }
    }
}
