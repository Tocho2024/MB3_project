package com.tocho.mb3

import GridComponent
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tocho.mb3.ui.theme.MB3Theme
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.net.Socket
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Locale

class AdminModeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get isOnline from the intent
        val isOnline = intent.getBooleanExtra("isOnline", false)

        setContent {
            MB3Theme {
                var currentScreen by remember { mutableStateOf("Grid") }
                val textFields = mutableMapOf<Int, MarkingItem>() // No state wrapping
                var showSaveDialog by remember { mutableStateOf(false) }
                var saveFileName by remember { mutableStateOf("") }

                when (currentScreen) {
                    "Grid" -> {
                        AdminModeScreen(
                            context = this,
                            isOnline = isOnline,
                            textFields = textFields,
                            onFileSelectionClick = {
                                // Show the dialog to get the filename
                                showSaveDialog = true
                            }
                        )

                        // Show the Save Dialog when triggered
                        if (showSaveDialog) {
                            AlertDialog(
                                onDismissRequest = { showSaveDialog = false },
                                title = { Text("Save Grid State") },
                                text = {
                                    OutlinedTextField(
                                        value = saveFileName,
                                        onValueChange = { saveFileName = it },
                                        label = { Text("Enter File Name") }
                                    )
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        if (saveFileName.isNotBlank()) {
                                            val data = textFields.map { "${it.key}:${it.value}" }.joinToString("\n")
                                            saveToFile(this, saveFileName, data)
                                            println("File saved as $saveFileName")
                                            showSaveDialog = false
                                            currentScreen = "FileSelection"
                                        }
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
                    "FileSelection" -> FileSelectionPage(
                        textFields = textFields, // Pass directly
                        onFileSaved = { filename, data -> saveToFile(this, filename, data) },
                        onFileSelected = { filename ->
                            val loadedTextFields = loadFromFile(this, filename)
                            println("Loaded text fields from file: $loadedTextFields") // Log loaded map

                            if (loadedTextFields != null) {
                                textFields.clear()
                                textFields.putAll(loadedTextFields)
                                println("Navigating to Grid with textFields: $textFields") // Log the data
                            } else {
                                println("Failed to load file: $filename")
                            }
                            println("Navigating to Grid with textFields: $textFields") // Log final state before navigation
                            currentScreen = "Grid"
                        },
                        onFileDeleted = { filename ->
                            deleteFile(this, filename)
                        }
                    )
                }
            }
        }
        /*
        setContent {
            MB3Theme {
                var currentScreen by remember { mutableStateOf("Grid") }
                //var textFields by remember { mutableStateOf(mutableMapOf<Int, MarkingItem>()) }
                val textFields = mutableMapOf<Int, MarkingItem>() // No state wrapping

                when (currentScreen) {
                    "Grid" -> AdminModeScreen(
                        context = this,
                        isOnline = isOnline,
                        textFields = textFields,
                        onFileSelectionClick = {
                            saveToFile(this, "current_state.txt", textFields.toString())
                            currentScreen = "FileSelection"
                        }
                    )
                    "FileSelection" -> FileSelectionPage(
                        textFields = textFields, // Pass directly
                        onFileSaved = { filename, data -> saveToFile(this, filename, data) },
                        onFileSelected = { filename ->
                            val loadedTextFields = loadFromFile(this, filename)
                            if (loadedTextFields != null) {
                                textFields.clear()
                                textFields.putAll(loadedTextFields)
                            }
                            currentScreen = "Grid"
                        },
                        onFileDeleted = { filename ->
                            deleteFile(this, filename)
                        }
                    )
                    /*
                    "FileSelection" -> FileSelectionPage(
                        textFields = textFields,
                        onFileSaved = { filename, data -> saveToFile(this, filename, data) },
                        onFileSelected = { filename ->
                            val loadedTextFields = loadFromFile(this, filename)
                            if (loadedTextFields != null) {
                                textFields.clear()
                                textFields.putAll(loadedTextFields)
                            }
                            currentScreen = "Grid"
                        }
                    )
                    */
                }
            }
            /*
            MB3Theme {
                AdminModeScreen(context = this, isOnline = isOnline)
            }
            */
        }
        */
    }

    // Updated function to get the local IP address using ConnectivityManager and LinkProperties
    private fun getLocalIpAddress(): String? {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return null
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val linkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return null
                val ipAddress = linkProperties.linkAddresses.firstOrNull { it.address is Inet4Address }?.address?.hostAddress
                return ipAddress
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Find the MB3 device by scanning the network
    private suspend fun findMB3Device(): String? {
        val localIpAddress = getLocalIpAddress()
        if (localIpAddress != null) {
            val subnet = localIpAddress.substringBeforeLast(".") // Get subnet (e.g., "192.168.1")
            var mb3Ip: String? = null

            // Launch a coroutine to scan the network for MB3
            mb3Ip = scanLocalNetwork(subnet) // now scanLocalNetwork returns the found MB3 IP or null

            return mb3Ip
        } else {
            println("Failed to get local IP address.")
            return null
        }
    }

    // Scan the local network for devices - now this function returns the found device IP address
    private suspend fun scanLocalNetwork(subnet: String): String? {
        return withContext(Dispatchers.IO) {
            for (i in 1..255) {
                val host = "$subnet.$i"
                try {
                    val inetAddress = InetAddress.getByName(host)
                    if (inetAddress.isReachable(500)) {
                        // Call the suspend function connectToDevice in a coroutine
                        if (connectToDevice(host)) {
                            return@withContext host // Found MB3
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return@withContext null // MB3 not found after scanning the entire subnet
        }
    }

    // Try to connect to a device (MB3) on a given IP
    private suspend fun connectToDevice(ipAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val port = 23 // MB3 port
                val socket = Socket(ipAddress, port)
                socket.close()
                true // Connection successful
            } catch (e: Exception) {
                false // Connection failed
            }
        }
    }

    // Define the function in AdminModeActivity
    suspend fun sendTextFieldsToMB3(textFields: Map<Int, MarkingItem>): String {
        return try {
            withContext(Dispatchers.IO) {
                val mb3IpAddress = findMB3Device() // Find MB3 device IP

                if (mb3IpAddress != null) {
                    val port = 23 // MB3 device port
                    println("Connecting to MB3 at IP: $mb3IpAddress on port: $port")

                    val socket = Socket(mb3IpAddress, port)
                    val outputStream: OutputStream = socket.getOutputStream()
                    val inputStream = socket.getInputStream()

                    // Helper function to read response
                    fun readResponse(inputStream: InputStream): String {
                        val responseBuffer = ByteArray(1024)
                        val bytesRead = inputStream.read(responseBuffer)
                        return String(responseBuffer, 0, bytesRead).trim()
                    }

                    // Construct the data string with text fields
                    val serialHeader = "//#Serial,0,1000,001,1,1,MAX,8:30,E,0,1000,001,1,1,MAX,8:30,E,0,1000,001,1,1,MAX,8:30,E\r\n"
                    val textFieldData = textFields.values.joinToString(separator = "\r\n") { textField ->
                        val xFormatted = String.format(Locale.US, "%.3f", textField.x.toFloat())
                        val yFormatted = String.format(Locale.US, "%.3f", textField.y.toFloat())
                        val spacingFormatted = String.format(Locale.US, "%.3f", textField.spacing)
                        "TEXT,F1,H${textField.height},W${textField.widthPercentage},x$xFormatted,y$yFormatted,A${textField.angle},p$spacingFormatted,f${textField.force},s${textField.quality},\"${textField.text}\""
                    }

                    val data = serialHeader + textFieldData + "\r\n"

                    // Step 1: Calculate the length of the data in bytes
                    val dataLength = data.toByteArray().size

                    // Step 2: Convert the length to hexadecimal and pad to 8 characters
                    val hexLength = dataLength.toString(16).padStart(8, '0')

                    // Step 3: Create the file write command with the correct length
                    val fileCommand = "@f_wfile$hexLength\"1:FILE/000.txt\"\r\n"
                    println("Calculated length: $dataLength (Hex: $hexLength)")
                    println("Sending command: $fileCommand")

                    // Step 4: Retry device status check using @inf command
                    val statusCommand = "@inf\r\n"
                    if (!retryDeviceStatus(statusCommand, outputStream, inputStream)) {
                        println("Device status check failed after retries. Proceeding anyway...")
                    }

                    // Step 5: Send the file write command
                    if (!retryFileWriteCommand(fileCommand, outputStream, inputStream)) {
                        println("MB3 did not acknowledge @f_wfile command after retries.")
                        socket.close()
                        return@withContext "Failed to send @f_wfile command. MB3 did not acknowledge."
                    }

                    // Step 6: Send the data
                    println("Sending data: $data")
                    outputStream.write(data.toByteArray())
                    outputStream.flush()

                    var response = readResponse(inputStream)
                    println("Response from MB3 (data): $response")
                    if (!response.contains("@ACK")) {
                        socket.close()
                        return@withContext "Failed to send data. MB3 did not acknowledge."
                    }

                    // Optional: Close socket after successful transmission
                    socket.close()

                    return@withContext "MB3 successfully acknowledged the file and data transmission."
                } else {
                    "Failed to find MB3 device on the network."
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Failed to send text field data: ${e.message ?: "Unknown error"}"
        }
    }

    suspend fun sendTestDataToMB3(): String {
        return try {
            withContext(Dispatchers.IO) {
                val mb3IpAddress = findMB3Device() // Find MB3 device IP

                if (mb3IpAddress != null) {
                    val port = 23 // MB3 device port
                    println("Connecting to MB3 at IP: $mb3IpAddress on port: $port")

                    val socket = Socket(mb3IpAddress, port)
                    val outputStream: OutputStream = socket.getOutputStream()
                    val inputStream = socket.getInputStream()

                    // Helper function to read response
                    fun readResponse(inputStream: InputStream): String {
                        val responseBuffer = ByteArray(1024)
                        val bytesRead = inputStream.read(responseBuffer)
                        return String(responseBuffer, 0, bytesRead).trim()
                    }

                    // Prepare the data to be sent
                    val data = "//#Serial,0,1000,001,1,1,MAX,8:30,E,0,1000,001,1,1,MAX,8:30,E,0,1000,001,1,1,MAX,8:30,E\r\n" +
                            "TEXT,F1,H10.0,W60,x0.000,y10.000,A0.00,p8.000,f0,s50,\"0123456789\"\r\n"

                    // Step 1: Calculate the length of the data in bytes
                    val dataLength = data.toByteArray().size

                    // Step 2: Convert the length to hexadecimal and pad to 8 characters
                    val hexLength = dataLength.toString(16).padStart(8, '0')

                    // Step 3: Create the file write command with the correct length
                    val fileCommand = "@f_wfile$hexLength\"1:FILE/000.txt\"\r\n"
                    println("Calculated length: $dataLength (Hex: $hexLength)")
                    println("Sending command: $fileCommand")

                    // Step 4: Retry device status check using @inf command
                    val statusCommand = "@inf\r\n"
                    if (!retryDeviceStatus(statusCommand, outputStream, inputStream)) {
                        println("Device status check failed after retries. Proceeding anyway...")
                    }

                    // Step 5: Send the file write command
                    if (!retryFileWriteCommand(fileCommand, outputStream, inputStream)) {
                        println("MB3 did not acknowledge @f_wfile command after retries.")
                        socket.close()
                        return@withContext "Failed to send @f_wfile command. MB3 did not acknowledge."
                    }

                    // Step 6: Send the data
                    println("Sending data: $data")
                    outputStream.write(data.toByteArray())
                    outputStream.flush()

                    val response = readResponse(inputStream)
                    println("Response from MB3 (data): $response")
                    if (!response.contains("@ACK")) {
                        socket.close()
                        return@withContext "Failed to send data. MB3 did not acknowledge."
                    }

                    // Optional: Close socket after successful transmission
                    socket.close()

                    return@withContext "MB3 successfully acknowledged the file and data transmission."

                } else {
                    return@withContext "Failed to find MB3 device on the network."
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Failed to send STM file: ${e.message ?: "Unknown error"}"
        }
    }

    // Function to retry @f_wfile command if necessary
    fun retryFileWriteCommand(command: String, outputStream: OutputStream, inputStream: InputStream, maxRetries: Int = 3, retryDelay: Long = 2000): Boolean {
        var retries = 0
        var response: String

        do {
            println("Sending command: $command")
            outputStream.write(command.toByteArray())
            outputStream.flush()

            // Read the response
            response = readResponse(inputStream)
            println("Response from MB3 (command): $response")

            if (response.contains("@ACK")) {
                return true // Command acknowledged, proceed
            }

            if (response.contains("@NAK")) {
                println("Received @NAK, retrying @f_wfile command in $retryDelay ms...")
                Thread.sleep(retryDelay)
            }

            retries++
        } while (retries < maxRetries)

        return false // Failed after retries
    }

    // Function to retry @inf status check command if necessary
    fun retryDeviceStatus(command: String, outputStream: OutputStream, inputStream: InputStream, maxRetries: Int = 3, retryDelay: Long = 2000): Boolean {
        var retries = 0
        var response: String

        do {
            println("Checking device status...")
            outputStream.write(command.toByteArray())
            outputStream.flush()

            // Read the response
            response = readResponse(inputStream)
            println("Device status: $response")

            if (!response.contains("@NAK")) {
                return true // Status is good, proceed
            }

            println("Device status returned @NAK, retrying in $retryDelay ms...")
            Thread.sleep(retryDelay)

            retries++
        } while (retries < maxRetries)

        return false // Failed after retries
    }

    // Move the readResponse function outside to be used in both functions
    fun readResponse(inputStream: InputStream): String {
        val responseBuffer = ByteArray(1024)
        val bytesRead = inputStream.read(responseBuffer)
        return String(responseBuffer, 0, bytesRead).trim()
    }

    private fun validateSTMData(data: String): Boolean {
        // Basic validation for STM data format

        // Check that the file starts with the correct prefix (e.g., "1:FILE")
        if (!data.startsWith("1:FILE")) {
            println("Invalid STM file header.")
            return false
        }

        // Check that it contains the required `TEXT` field
        if (!data.contains("TEXT")) {
            println("STM file is missing TEXT field.")
            return false
        }

        // Additional validation checks can be added here:
        // - Ensure commas and structure are correct
        // - Ensure position and formatting are well-defined

        return true
    }

    // Log Wi-Fi information for debugging purposes
    private fun logWifiInfo(context: Context) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return

            // Check if the active network is Wi-Fi
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val linkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return
                val ssid = linkProperties.interfaceName  // This provides the network interface (usually Wi-Fi)

                // Get the IP address from the Wi-Fi network
                val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                for (networkInterface in networkInterfaces) {
                    if (networkInterface.name == ssid) { // Match the interface name to the SSID
                        val inetAddresses = networkInterface.inetAddresses
                        for (inetAddress in inetAddresses) {
                            if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                                // Log the Wi-Fi SSID and IP address
                                println("Connected to Wi-Fi interface: $ssid")
                                println("IP Address: ${inetAddress.hostAddress}")
                                return
                            }
                        }
                    }
                }
            } else {
                println("Not connected to a Wi-Fi network")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to retrieve Wi-Fi info: ${e.message}")
        }
    }
}

@Composable
fun AdminModeScreen(
    context: AdminModeActivity,
    isOnline: Boolean,
    textFields: MutableMap<Int, MarkingItem>,
    onFileSelectionClick: () -> Unit // Pass callback to navigate
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedTextField by remember { mutableStateOf<MarkingItem?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Display online/offline status at the top
        Text(
            text = if (isOnline) "Admin Mode: Online" else "Admin Mode: Offline",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(8.dp)
        )

        // Pass the isOnline status and sendTestDataToMB3 to GridComponent
        GridComponent(
            textFields = textFields,
            onCellClick = { index ->
                val newTextField = MarkingItem(
                    text = "Text",
                    x = index % 60,
                    y = index / 60,
                    height = 5,
                    angle = 0f,
                    spacing = 0f,
                    widthPercentage = 60f,
                    force = 50,
                    quality = 100
                )
                textFields[index] = newTextField
                selectedTextField = newTextField
                showDialog = true
            },
            onTextClick = { index, textField ->
                selectedTextField = textField
                showDialog = true
            },
            sendTestDataToMB3 = {
                coroutineScope.launch {
                    val result = context.sendTestDataToMB3()
                    Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                }
            },
            sendTextFieldsToMB3 = {
                coroutineScope.launch {
                    val result = context.sendTextFieldsToMB3(textFields)
                    Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                }
            },
            onFileSelectionClick = onFileSelectionClick // Use callback to navigate
        )

        if (showDialog && selectedTextField != null) {
            EditTextDialog(
                textField = selectedTextField!!,
                onUpdate = { updatedTextField ->
                    val newIndex = updatedTextField.y * 60 + updatedTextField.x
                    textFields.remove(selectedTextField!!.y * 60 + selectedTextField!!.x)
                    textFields[newIndex] = updatedTextField
                    showDialog = false
                },
                onDismiss = {
                    showDialog = false
                }
            )
        }
    }
}

//@Composable
//fun AdminModeScreen(context: AdminModeActivity, isOnline: Boolean) {
//    var showDialog by remember { mutableStateOf(false) }
//    var selectedTextField by remember { mutableStateOf<MarkingItem?>(null) }
//    var textFields by remember { mutableStateOf(mutableMapOf<Int, MarkingItem>()) }
//
//    val coroutineScope = rememberCoroutineScope()
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        // Display online/offline status at the top
//        Text(
//            text = if (isOnline) "Admin Mode: Online" else "Admin Mode: Offline",
//            style = MaterialTheme.typography.headlineSmall,
//            modifier = Modifier.padding(8.dp)
//        )
//
//        // Pass the isOnline status and sendTestDataToMB3 to GridComponent
//        GridComponent(
////            rows = 20,
////            columns = 80,
//            textFields = textFields,
//            onCellClick = { index ->
//                val newTextField = MarkingItem(
//                    text = "Text",
//                    x = index % 60,
//                    y = index / 60,
//                    height = 5,
//                    angle = 0f,
//                    spacing = 0f,
//                    widthPercentage = 60f,
//                    force = 50,
//                    quality = 100
//                )
//                textFields[index] = newTextField
//                selectedTextField = newTextField
//                showDialog = true
//            },
//            onTextClick = { index, textField ->
//                selectedTextField = textField
//                showDialog = true
//            },
//            sendTestDataToMB3 = {
//                coroutineScope.launch {
//                    val result = context.sendTestDataToMB3()
//                    Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
//                }
//            },
//            sendTextFieldsToMB3 = {
//                coroutineScope.launch {
//                    val result = context.sendTextFieldsToMB3(textFields) // Pass the 'textFields' map here
//                    Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
//                }
//            },
//            onFileSelectionClick = {
//                saveToFile(context, "current_state.txt", textFields.toString()) // Save current grid data
//                currentScreen = "FileSelection"
//            }
//        )
//
//        if (showDialog && selectedTextField != null) {
//            EditTextDialog(
//                textField = selectedTextField!!,
//                onUpdate = { updatedTextField ->
//                    val newIndex = updatedTextField.y * 60 + updatedTextField.x
//                    textFields.remove(selectedTextField!!.y * 60 + selectedTextField!!.x)
//                    textFields[newIndex] = updatedTextField
//                    showDialog = false
//                },
//                onDismiss = {
//                    showDialog = false
//                }
//            )
//        }
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTextDialog(
    textField: MarkingItem,
    onUpdate: (MarkingItem) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(textField.text) }
    var height by remember { mutableStateOf(textField.height.toString()) }
    var angle by remember { mutableStateOf(textField.angle.toString()) }
    var x by remember { mutableStateOf(textField.x.toString()) }
    var y by remember { mutableStateOf(textField.y.toString()) }
    var spacing by remember { mutableStateOf(textField.spacing.toString()) }
    var widthPercentage by remember { mutableStateOf(textField.widthPercentage.toString()) }
    var force by remember { mutableStateOf(textField.force.toString()) }
    var quality by remember { mutableStateOf(textField.quality.toString()) }

    // State for font selection
    var fontSelection by remember { mutableStateOf(textField.font) }
    var expanded by remember { mutableStateOf(false) } // Dropdown expanded state

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            confirmButton = {
                Button(onClick = {
                    onUpdate(
                        textField.copy(
                            text = text,
                            height = height.toIntOrNull() ?: 0,
                            angle = angle.toFloatOrNull()?.coerceIn(0f, 360f) ?: 0f,
                            x = x.toIntOrNull() ?: 0,
                            y = y.toIntOrNull() ?: 0,
                            spacing = spacing.toFloatOrNull() ?: 0f,
                            widthPercentage = widthPercentage.toFloatOrNull() ?: 1f,
                            force = force.toIntOrNull()?.coerceIn(0, 99) ?: 50,
                            quality = quality.toIntOrNull()?.coerceIn(1, 99) ?: 100,
                            font = fontSelection  // Update font choice
                        )
                    )
                    onDismiss()
                }) {
                    Text("Update")
                }
            },
            dismissButton = {
                Button(onClick = { onDismiss() }) {
                    Text("Cancel")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    TextField(value = text, onValueChange = { text = it }, label = { Text("Text") })
                    TextField(value = height, onValueChange = { height = it }, label = { Text("Height") })
                    TextField(
                        value = angle,
                        onValueChange = {
                            angle = it.toFloatOrNull()?.takeIf { it in 0f..360f }?.toString() ?: "0.00"
                        },
                        label = { Text("Angle") }
                    )
                    TextField(value = x, onValueChange = { x = it }, label = { Text("X Coordinate") })
                    TextField(value = y, onValueChange = { y = it }, label = { Text("Y Coordinate") })
                    TextField(value = spacing, onValueChange = { spacing = it }, label = { Text("Spacing") })
                    TextField(value = widthPercentage, onValueChange = { widthPercentage = it }, label = { Text("Width (%)") })
                    TextField(
                        value = force,
                        onValueChange = {
                            val clampedValue = it.toIntOrNull()?.coerceIn(0, 99) ?: 0
                            force = clampedValue.toString()
                        },
                        label = { Text("Marking Force") }
                    )
                    TextField(
                        value = quality,
                        onValueChange = {
                            val clampedValue = it.toIntOrNull()?.coerceIn(1, 99) ?: 1
                            quality = clampedValue.toString()
                        },
                        label = { Text("Marking Quality") }
                    )

                    // Dropdown for font selection
                    Text("Font")
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        TextField(
                            value = fontSelection,
                            onValueChange = {},
                            label = { Text("Font") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf("F1", "F2", "F3").forEach { font ->
                                DropdownMenuItem(
                                    text = { Text(font) },
                                    onClick = {
                                        fontSelection = font
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}

/*
@Composable
fun EditTextDialog(
    textField: MarkingItem,
    onUpdate: (MarkingItem) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(textField.text) }
    var height by remember { mutableStateOf(textField.height.toString()) }
    var angle by remember { mutableStateOf(textField.angle.toString()) }
    var x by remember { mutableStateOf(textField.x.toString()) }
    var y by remember { mutableStateOf(textField.y.toString()) }
    var spacing by remember { mutableStateOf(textField.spacing.toString()) }
    var widthPercentage by remember { mutableStateOf(textField.widthPercentage.toString()) }
    var force by remember { mutableStateOf(textField.force.toString()) }
    var quality by remember { mutableStateOf(textField.quality.toString()) }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            confirmButton = {
                Button(onClick = {
                    onUpdate(
                        textField.copy(
                            text = text,
                            height = height.toIntOrNull() ?: 0,
                            angle = angle.toFloatOrNull() ?: 0f,
                            x = x.toIntOrNull() ?: 0,
                            y = y.toIntOrNull() ?: 0,
                            spacing = spacing.toFloatOrNull() ?: 0f,
                            widthPercentage = widthPercentage.toFloatOrNull() ?: 1f,
                            force = force.toIntOrNull() ?: 50,
                            quality = quality.toIntOrNull() ?: 100
                        )
                    )
                    onDismiss()
                }) {
                    Text("Update")
                }
            },
            dismissButton = {
                Button(onClick = { onDismiss() }) {
                    Text("Cancel")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    TextField(value = text, onValueChange = { text = it }, label = { Text("Text") })
                    TextField(value = height, onValueChange = { height = it }, label = { Text("Height") })
                    TextField(value = angle, onValueChange = { angle = it }, label = { Text("Angle") })
                    TextField(value = x, onValueChange = { x = it }, label = { Text("X Coordinate") })
                    TextField(value = y, onValueChange = { y = it }, label = { Text("Y Coordinate") })
                    TextField(value = spacing, onValueChange = { spacing = it }, label = { Text("Spacing") })
                    TextField(value = widthPercentage, onValueChange = { widthPercentage = it }, label = { Text("Width (%)") })
                    TextField(value = force, onValueChange = { force = it }, label = { Text("Marking Force") })
                    TextField(value = quality, onValueChange = { quality = it }, label = { Text("Marking Quality") })
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}
 */