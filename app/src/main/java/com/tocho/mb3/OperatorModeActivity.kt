package com.tocho.mb3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tocho.mb3.ui.theme.MB3Theme

class OperatorModeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MB3Theme {
                OperatorModeScreen()
            }
        }
    }
}

@Composable
fun OperatorModeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Operator Mode: Read/Send Files")

        // Button to read files
        Button(
            onClick = {
                // TODO: Implement file reading functionality
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Read File")
        }

        // Button to send files
        Button(
            onClick = {
                // TODO: Implement file sending functionality
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Send File")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewOperatorModeScreen() {
    MB3Theme {
        OperatorModeScreen()
    }
}