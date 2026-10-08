package com.rrblack.meowtranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.rrblack.meowtranslator.ui.MeowApp
import com.rrblack.meowtranslator.ui.theme.MeowTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeowTheme {
                MeowApp(viewModel)
            }
        }
    }
}
