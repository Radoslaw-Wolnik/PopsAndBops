package com.example.popsandbops

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.popsandbops.ui.PopsAndBopsApp
import com.example.popsandbops.ui.theme.PopsAndBopsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PopsAndBopsTheme {
                PopsAndBopsApp()
            }
        }
    }
}

@Composable
@Preview
@Composable
fun PopsAndBopsPreview() {
    PopsAndBopsTheme {
        PopsAndBopsApp()
    }
}
