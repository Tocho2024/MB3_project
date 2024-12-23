package com.tocho.mb3

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.File
import com.tocho.mb3.MarkingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSelectionPage(
    textFields: MutableMap<Int, MarkingItem>,
    onFileSaved: (String, String) -> Unit,
    onFileSelected: (String) -> Unit,
    onFileDeleted: (String) -> Unit
) {
    val context = LocalContext.current
    val filesDir = context.filesDir
    var fileList by remember { mutableStateOf(filesDir.listFiles()?.map { it.name } ?: emptyList()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Selection") },
                actions = {
                    // Plus button to add a new file
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add File")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Show the list of saved files
            LazyColumn {
                items(fileList) { fileName ->
                    FileItem(
                        fileName = fileName,
                        onLoad = { onFileSelected(fileName) },
                        onDelete = {
                            onFileDeleted(fileName)
                            fileList = filesDir.listFiles()?.map { it.name } ?: emptyList()
                        }
                    )
                }
            }
        }
    }

    // Dialog for saving a new file
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save File") },
//            text = {
//                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
//                    OutlinedTextField(
//                        value = newFileName,
//                        onValueChange = { newFileName = it },
//                        label = { Text("Enter File Name") }
//                    )
//                }
//            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding() // Adjusts for the keyboard
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Enter File Name") },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth() // Ensure the field spans the dialog width
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Serialize textFields map to a string
                    val data = textFields.entries.joinToString("\n") { "${it.key}:${it.value.toString()}" }
                    println("Saving to file: $newFileName with content:\n$data") // Additional log for clarity

                    // Save the file
                    onFileSaved(newFileName, data)

                    // Refresh the file list
                    fileList = filesDir.listFiles()?.map { it.name } ?: emptyList()

                    // Reset dialog state
                    newFileName = ""
                    showSaveDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FileItem(
    fileName: String,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onLoad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete File")
        }
    }
}

// Helper function to save data to a file
fun saveToFile(context: Context, filename: String, data: String) {
    try {
        val file = File(context.filesDir, filename)
        file.writeText(data)
        println("Data successfully saved to $filename:\n$data")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Helper function to load data from a file
fun loadFromFile(context: Context, filename: String): MutableMap<Int, MarkingItem>? {
    return try {
        val file = File(context.filesDir, filename)
        if (file.exists()) {
            val content = file.readText()
            println("Loaded file content:\n$content") // Log raw file content
            println("Loaded raw content:\n${file.readText()}")

            val map = mutableMapOf<Int, MarkingItem>()
            for (line in content.lines()) {
                val parts = line.split(":")
                if (parts.size == 2) {
                    val key = parts[0].toIntOrNull()
                    val value = MarkingItem.fromString(parts[1])
                    if (key != null && value != null) {
                        map[key] = value
                    }
                }
            }
            println("Parsed text fields: $map") // Log parsed text fields
            map
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun deleteFile(context: Context, filename: String) {
    try {
        val file = File(context.filesDir, filename)
        if (file.exists()) {
            file.delete()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/*
@Composable
fun FileSelectionPage(
    textFields: MutableMap<Int, MarkingItem>,
    onFileSaved: (String, String) -> Unit,
    onFileSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val filesDir = context.filesDir
    val fileList = remember { mutableStateOf(filesDir.listFiles()?.map { it.name } ?: emptyList()) }
    var filename by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "File Selection",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Input for file name
        OutlinedTextField(
            value = filename,
            onValueChange = { filename = it },
            label = { Text("Enter File Name") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // Save Button
        Button(
            onClick = {
                val data = textFields.map { "${it.key}:${it.value}" }.joinToString("\n")
                if (filename.isNotBlank()) {
                    onFileSaved(filename, data)
                    fileList.value = filesDir.listFiles()?.map { it.name } ?: emptyList()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Save Current Grid")
        }

        // List existing files
        Text(
            "Saved Files",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        fileList.value.forEach { file ->
            Button(
                onClick = { onFileSelected(file) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(file)
            }
        }
    }
}

// Helper function to save data to a file
fun saveToFile(context: Context, filename: String, data: String) {
    try {
        val file = File(context.filesDir, filename)
        file.writeText(data)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Helper function to load data from a file
fun loadFromFile(context: Context, filename: String): MutableMap<Int, MarkingItem>? {
    return try {
        val file = File(context.filesDir, filename)
        if (file.exists()) {
            val lines = file.readLines()
            val map = mutableMapOf<Int, MarkingItem>()
            for (line in lines) {
                val parts = line.split(":")
                if (parts.size == 2) {
                    val key = parts[0].toIntOrNull()
                    val value = MarkingItem.fromString(parts[1])
                    if (key != null && value != null) {
                        map[key] = value
                    }
                }
            }
            map
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Preview for UI testing
@Preview(showBackground = true)
@Composable
fun PreviewFileSelectionPage() {
    val sampleData = mutableMapOf(
        1 to MarkingItem("Hello", 1, 2, 20, 50f, 0f, 0f, 100, 1)
    )
    FileSelectionPage(
        textFields = sampleData,
        onFileSaved = { _, _ -> },
        onFileSelected = {}
    )
}
*/


/*
@Composable
fun FileSelectionPage(
    textFields: MutableMap<Int, MarkingItem>,
    onFileSaved: (String, String) -> Unit,
    onFileSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val filesDir = context.filesDir
    val fileList = remember { mutableStateOf(filesDir.listFiles()?.map { it.name } ?: emptyList()) }
    var filename by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "File Selection",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Input for file name
        OutlinedTextField(
            value = filename,
            onValueChange = { filename = it },
            label = { Text("Enter File Name") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // Save Button
        Button(
            onClick = {
                val data = textFields.map { "${it.key}:${it.value}" }.joinToString("\n")
                if (filename.isNotBlank()) {
                    onFileSaved(filename, data)
                    fileList.value = filesDir.listFiles()?.map { it.name } ?: emptyList()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Save Current Grid")
        }

        // List existing files
        Text(
            "Saved Files",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        fileList.value.forEach { file ->
            Button(
                onClick = { onFileSelected(file) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(file)
            }
        }
    }
}
*/