package com.cihatakyol.case_study

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cihatakyol.case_study.ui.navigation.AppNavHost
import com.cihatakyol.case_study.ui.theme.CasestudyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CasestudyTheme {
                AppNavHost()
            }
        }
    }
}
