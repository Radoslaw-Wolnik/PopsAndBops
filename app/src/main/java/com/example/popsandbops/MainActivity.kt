package com.example.popsandbops

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.popsandbops.ui.PopsAndBopsApp
import com.example.popsandbops.ui.theme.PopsAndBopsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_PopsAndBops)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.paper))
                post {
                    this@MainActivity.setContent {
                        PopsAndBopsTheme {
                            PopsAndBopsApp()
                        }
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun PopsAndBopsPreview() {
    PopsAndBopsTheme {
        PopsAndBopsApp()
    }
}
