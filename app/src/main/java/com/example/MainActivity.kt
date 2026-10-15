package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.MayaScreen
import com.example.ui.theme.MayaMidnight
import com.example.ui.theme.MayaTheme
import com.example.ui.viewmodel.MayaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MayaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MayaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MayaMidnight
                ) {
                    MayaScreen(viewModel = viewModel)
                }
            }
        }
    }
}
