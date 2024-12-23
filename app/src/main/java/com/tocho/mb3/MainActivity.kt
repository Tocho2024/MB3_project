package com.tocho.mb3

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tocho.mb3.ui.theme.MB3Theme

class MainActivity : ComponentActivity() {

    // Use a state variable to track whether the device is connected to the correct Wi-Fi
    private var isMb3NetworkAvailable by mutableStateOf(false)

    // Modern permission request using ActivityResultContracts
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_WIFI_STATE] == true) {
                // Permissions granted
                monitorWifiNetwork(this)
            } else {
                Log.d("MainActivity", "Permissions denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for permissions and start Wi-Fi monitoring if granted
        checkPermissionsAndMonitorWifi()

        setContent {
            MB3Theme {
                var showAdminDialog by remember { mutableStateOf(false) }
                var showOperatorDialog by remember { mutableStateOf(false) }

                // Show mode selection screen
                ModeSelectionScreen(
                    onOperatorModeSelected = {
                        showOperatorDialog = true  // Show dialog for Operator mode
                    },
                    onAdminModeSelected = {
                        showAdminDialog = true  // Trigger dialog for Admin Mode selection
                    }
                )

                // Show online/offline dialog for Admin mode
                if (showAdminDialog) {
                    AdminModeDialog(
                        onDismiss = { showAdminDialog = false },
                        onModeSelected = { isOnline ->
                            showAdminDialog = false
                            val intent = Intent(this, AdminModeActivity::class.java)
                            intent.putExtra("isOnline", isOnline)
                            startActivity(intent)
                        },
                        isOnlineEnabled = isMb3NetworkAvailable
                    )
                }

                // Show online/offline dialog for Operator mode
                if (showOperatorDialog) {
                    OperatorModeDialog(
                        onDismiss = { showOperatorDialog = false },
                        onModeSelected = { isOnline ->
                            showOperatorDialog = false
                            val intent = Intent(this, AdminModeActivity::class.java)
                            intent.putExtra("isOnline", isOnline)
                            startActivity(intent)
                        },
                        isOnlineEnabled = isMb3NetworkAvailable
                    )
                }
            }
        }
    }

    // Function to check permissions and monitor Wi-Fi networks if permissions are granted
    private fun checkPermissionsAndMonitorWifi() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE
        )

        val permissionGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (!permissionGranted) {
            // Request permissions if not granted
            requestMultiplePermissions.launch(permissions)
        } else {
            // If permissions are granted, start monitoring the Wi-Fi network
            monitorWifiNetwork(this)
        }
    }

    // Function to monitor Wi-Fi connections using ConnectivityManager
    private fun monitorWifiNetwork(context: Context) {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val wifiNetworkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)

                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    @Suppress("DEPRECATION")
                    val connectionInfo = wifiManager.connectionInfo
                    val currentSSID = connectionInfo.ssid?.removePrefix("\"")?.removeSuffix("\"")

                    isMb3NetworkAvailable = currentSSID?.contains("MB3", ignoreCase = true) == true
                    Log.d("MainActivity", "Connected to SSID: $currentSSID")
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    isMb3NetworkAvailable = false
                    Log.d("MainActivity", "Wi-Fi network lost.")
                }
            }

            connectivityManager.registerNetworkCallback(wifiNetworkRequest, networkCallback)

        } catch (e: SecurityException) {
            Log.e("MainActivity", "Wi-Fi monitoring failed due to missing permissions", e)
        }
    }
}

@Composable
fun ModeSelectionScreen(
    onOperatorModeSelected: () -> Unit,
    onAdminModeSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display the logo above the buttons
        Image(
            painter = painterResource(id = R.drawable.mb3logotemp), // Replace with your actual image name
            contentDescription = "App Logo",
            modifier = Modifier
                .size(150.dp) // Adjust the size of the logo
                .padding(bottom = 32.dp)
        )

        // Operator Mode Button
        Button(
            onClick = { onOperatorModeSelected() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(stringResource(R.string.operator_mode))
        }

        // Administrator Mode Button
        Button(
            onClick = { onAdminModeSelected() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.administrator_mode))
        }
    }
}

@Composable
fun AdminModeDialog(
    onDismiss: () -> Unit,
    onModeSelected: (Boolean) -> Unit,
    isOnlineEnabled: Boolean
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.select_admin_mode)) },
        text = {
            Column {
                Text(stringResource(R.string.choose_online_offline_admin))
                if (!isOnlineEnabled) {
                    Text(
                        text = stringResource(R.string.not_connected_mb3),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onModeSelected(true) },
                enabled = isOnlineEnabled  // Enable the button only if connected to MB3
            ) {
                Text(stringResource(R.string.online))
            }
        },
        dismissButton = {
            Button(onClick = {
                onModeSelected(false)  // Offline mode selected
            }) {
                Text(stringResource(R.string.offline))
            }
        }
    )
}

@Composable
fun OperatorModeDialog(
    onDismiss: () -> Unit,
    onModeSelected: (Boolean) -> Unit,
    isOnlineEnabled: Boolean
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.select_operator_mode)) },
        text = {
            Column {
                Text(stringResource(R.string.choose_online_offline_operator))
                if (!isOnlineEnabled) {
                    Text(
                        text = stringResource(R.string.not_connected_mb3),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onModeSelected(true) },
                enabled = isOnlineEnabled  // Enable the button only if connected to MB3
            ) {
                Text(stringResource(R.string.online))
            }
        },
        dismissButton = {
            Button(onClick = {
                onModeSelected(false)  // Offline mode selected
            }) {
                Text(stringResource(R.string.offline))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewModeSelectionScreen() {
    MB3Theme {
        ModeSelectionScreen({}, {})
    }
}


//package com.tocho.mb3
//
//import android.Manifest
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.ConnectivityManager
//import android.net.Network
//import android.net.NetworkCapabilities
//import android.net.NetworkRequest
//import android.net.wifi.WifiManager
//import android.os.Bundle
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.core.content.ContextCompat
//import com.tocho.mb3.ui.theme.MB3Theme
//
//class MainActivity : ComponentActivity() {
//
//    // Use a state variable to track whether the device is connected to the correct Wi-Fi
//    private var isMb3NetworkAvailable by mutableStateOf(false)
//
//    // Modern permission request using ActivityResultContracts
//    private val requestMultiplePermissions =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
//                permissions[Manifest.permission.ACCESS_WIFI_STATE] == true) {
//                // Permissions granted
//                monitorWifiNetwork(this)
//            } else {
//                Log.d("MainActivity", "Permissions denied.")
//            }
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Check for permissions and start Wi-Fi monitoring if granted
//        checkPermissionsAndMonitorWifi()
//
//        setContent {
//            MB3Theme {
//                var showAdminDialog by remember { mutableStateOf(false) }
//                var showOperatorDialog by remember { mutableStateOf(false) }
//
//                // Show mode selection screen
//                ModeSelectionScreen(
//                    onOperatorModeSelected = {
//                        showOperatorDialog = true  // Show dialog for Operator mode
//                    },
//                    onAdminModeSelected = {
//                        showAdminDialog = true  // Trigger dialog for Admin Mode selection
//                    }
//                )
//
//                // Show online/offline dialog for Admin mode
//                if (showAdminDialog) {
//                    AdminModeDialog(
//                        onDismiss = { showAdminDialog = false },
//                        onModeSelected = { isOnline ->
//                            showAdminDialog = false
//                            val intent = Intent(this, AdminModeActivity::class.java)
//                            intent.putExtra("isOnline", isOnline)
//                            startActivity(intent)
//                        },
//                        isOnlineEnabled = isMb3NetworkAvailable  // Enable/disable Online mode
//                    )
//                }
//
//                // Show online/offline dialog for Operator mode
//                if (showOperatorDialog) {
//                    OperatorModeDialog(
//                        onDismiss = { showOperatorDialog = false },
//                        onModeSelected = { isOnline ->
//                            showOperatorDialog = false
//                            val intent = Intent(this, AdminModeActivity::class.java)
//                            intent.putExtra("isOnline", isOnline)
//                            startActivity(intent)
//                        },
//                        isOnlineEnabled = isMb3NetworkAvailable  // Enable/disable Online mode
//                    )
//                }
//            }
//        }
//    }
//
//    // Function to check permissions and monitor Wi-Fi networks if permissions are granted
//    private fun checkPermissionsAndMonitorWifi() {
//        val permissions = arrayOf(
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.ACCESS_WIFI_STATE
//        )
//
//        val permissionGranted = permissions.all { permission ->
//            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
//        }
//
//        if (!permissionGranted) {
//            // Request permissions if not granted
//            requestMultiplePermissions.launch(permissions)
//        } else {
//            // Permissions are already granted, start monitoring Wi-Fi networks
//            monitorWifiNetwork(this)
//        }
//    }
//
//    // Function to monitor Wi-Fi connections using ConnectivityManager
//    @Suppress("DEPRECATION")  // Suppress the deprecated warning for SSID
//    private fun monitorWifiNetwork(context: Context) {
//        try {
//            val connectivityManager =
//                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//            val wifiNetworkRequest = NetworkRequest.Builder()
//                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//                .build()
//
//            val networkCallback = object : ConnectivityManager.NetworkCallback() {
//                override fun onAvailable(network: Network) {
//                    super.onAvailable(network)
//
//                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//                    @Suppress("DEPRECATION")
//                    val connectionInfo = wifiManager.connectionInfo  // Use for backward compatibility
//                    val currentSSID = connectionInfo.ssid?.removePrefix("\"")?.removeSuffix("\"")
//
//                    // Modern approach: use getCurrentNetwork() (API 29+) if available
//                    isMb3NetworkAvailable = currentSSID?.contains("MB3", ignoreCase = true) == true
//
//                    Log.d("MainActivity", "Connected to SSID: $currentSSID")
//                }
//
//                override fun onLost(network: Network) {
//                    super.onLost(network)
//                    isMb3NetworkAvailable = false
//                    Log.d("MainActivity", "Wi-Fi network lost.")
//                }
//            }
//
//            connectivityManager.registerNetworkCallback(wifiNetworkRequest, networkCallback)
//
//        } catch (e: SecurityException) {
//            Log.e("MainActivity", "Wi-Fi monitoring failed due to missing permissions", e)
//        }
//    }
//}
//
//@Composable
//fun ModeSelectionScreen(
//    onOperatorModeSelected: () -> Unit,
//    onAdminModeSelected: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        // Display the logo above the buttons
//        Image(
//            painter = painterResource(id = R.drawable.mb3logotemp), // Replace with your actual image name
//            contentDescription = "App Logo",
//            modifier = Modifier
//                .size(150.dp) // Adjust the size of the logo
//                .padding(bottom = 32.dp)
//        )
//
//        // Operator Mode Button
//        Button(
//            onClick = { onOperatorModeSelected() },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 16.dp)
//        ) {
//            Text(text = "Operator Mode")
//        }
//
//        // Administrator Mode Button
//        Button(
//            onClick = { onAdminModeSelected() },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text(text = "Administrator Mode")
//        }
//    }
//}
//
//@Composable
//fun AdminModeDialog(onDismiss: () -> Unit, onModeSelected: (Boolean) -> Unit, isOnlineEnabled: Boolean) {
//    AlertDialog(
//        onDismissRequest = { onDismiss() },
//        title = { Text("Select Admin Mode") },
//        text = { Text("Choose between online or offline mode for Admin Mode.") },
//        confirmButton = {
//            Button(
//                onClick = { onModeSelected(true) },
//                enabled = isOnlineEnabled  // Enable the button only if the network is available
//            ) {
//                Text("Online")
//            }
//        },
//        dismissButton = {
//            Button(onClick = {
//                onModeSelected(false)  // Offline mode selected
//            }) {
//                Text("Offline")
//            }
//        }
//    )
//}
//
//@Composable
//fun OperatorModeDialog(onDismiss: () -> Unit, onModeSelected: (Boolean) -> Unit, isOnlineEnabled: Boolean) {
//    AlertDialog(
//        onDismissRequest = { onDismiss() },
//        title = { Text("Select Operator Mode") },
//        text = { Text("Choose between online or offline mode for Operator Mode.") },
//        confirmButton = {
//            Button(
//                onClick = { onModeSelected(true) },
//                enabled = isOnlineEnabled  // Enable the button only if the network is available
//            ) {
//                Text("Online")
//            }
//        },
//        dismissButton = {
//            Button(onClick = {
//                onModeSelected(false)  // Offline mode selected
//            }) {
//                Text("Offline")
//            }
//        }
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun PreviewModeSelectionScreen() {
//    MB3Theme {
//        ModeSelectionScreen({}, {})
//    }
//}


//package com.tocho.mb3
//
//import android.Manifest
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.ConnectivityManager
//import android.net.Network
//import android.net.NetworkCapabilities
//import android.net.NetworkRequest
//import android.net.wifi.WifiManager
//import android.os.Bundle
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.core.content.ContextCompat
//import com.tocho.mb3.ui.theme.MB3Theme
//
//class MainActivity : ComponentActivity() {
//
//    // Use a state variable to track whether the device is connected to the correct Wi-Fi
//    private var isMb3NetworkAvailable by mutableStateOf(false)
//
//    // Modern permission request using ActivityResultContracts
//    private val requestMultiplePermissions =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
//                permissions[Manifest.permission.ACCESS_WIFI_STATE] == true) {
//                // Permissions granted
//                monitorWifiNetwork(this)
//            } else {
//                Log.d("MainActivity", "Permissions denied.")
//            }
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Check for permissions and start Wi-Fi monitoring if granted
//        checkPermissionsAndMonitorWifi()
//
//        setContent {
//            MB3Theme {
//                var showAdminDialog by remember { mutableStateOf(false) }
//
//                // Show mode selection screen
//                ModeSelectionScreen(
//                    onOperatorModeSelected = {
//                        val intent = Intent(this, AdminModeActivity::class.java)
//                        intent.putExtra("isOnline", true)  // Or false based on the mode
//                        startActivity(intent)
//                    },
//                    onAdminModeSelected = {
//                        showAdminDialog = true  // Trigger dialog for Admin Mode selection
//                    }
//                )
//
//                // Show online/offline dialog when showAdminDialog is true
//                if (showAdminDialog) {
//                    AdminModeDialog(
//                        onDismiss = { showAdminDialog = false },
//                        onModeSelected = { isOnline ->
//                            showAdminDialog = false
//                            val intent = Intent(this, AdminModeActivity::class.java)
//                            intent.putExtra("isOnline", isOnline)
//                            startActivity(intent)
//                        },
//                        isOnlineEnabled = isMb3NetworkAvailable  // Enable/disable Online mode
//                    )
//                }
//            }
//        }
//    }
//
//    // Function to check permissions and monitor Wi-Fi networks if permissions are granted
//    private fun checkPermissionsAndMonitorWifi() {
//        val permissions = arrayOf(
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.ACCESS_WIFI_STATE
//        )
//
//        val permissionGranted = permissions.all { permission ->
//            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
//        }
//
//        if (!permissionGranted) {
//            // Request permissions if not granted
//            requestMultiplePermissions.launch(permissions)
//        } else {
//            // Permissions are already granted, start monitoring Wi-Fi networks
//            monitorWifiNetwork(this)
//        }
//    }
//
//    // Function to monitor Wi-Fi connections using ConnectivityManager
//    @Suppress("DEPRECATION")  // Suppress the deprecated warning for SSID
//    private fun monitorWifiNetwork(context: Context) {
//        try {
//            val connectivityManager =
//                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//            val wifiNetworkRequest = NetworkRequest.Builder()
//                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//                .build()
//
//            val networkCallback = object : ConnectivityManager.NetworkCallback() {
//                override fun onAvailable(network: Network) {
//                    super.onAvailable(network)
//
//                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//                    @Suppress("DEPRECATION")
//                    val connectionInfo = wifiManager.connectionInfo
//                    val currentSSID = connectionInfo.ssid
//
//                    isMb3NetworkAvailable = currentSSID.contains("MB3", ignoreCase = true)
//
//                    Log.d("MainActivity", "Connected to SSID: $currentSSID")
//                }
//
//                override fun onLost(network: Network) {
//                    super.onLost(network)
//                    isMb3NetworkAvailable = false
//                    Log.d("MainActivity", "Wi-Fi network lost.")
//                }
//            }
//
//            connectivityManager.registerNetworkCallback(wifiNetworkRequest, networkCallback)
//
//        } catch (e: SecurityException) {
//            Log.e("MainActivity", "Wi-Fi monitoring failed due to missing permissions", e)
//        }
//    }
//}
//
//@Composable
//fun ModeSelectionScreen(
//    onOperatorModeSelected: () -> Unit,
//    onAdminModeSelected: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        // Display the logo above the buttons
//        Image(
//            painter = painterResource(id = R.drawable.mb3logotemp), // Replace with your actual image name
//            contentDescription = "App Logo",
//            modifier = Modifier
//                .size(150.dp) // Adjust the size of the logo
//                .padding(bottom = 32.dp)
//        )
//
//        // Operator Mode Button
//        Button(
//            onClick = { onOperatorModeSelected() },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 16.dp)
//        ) {
//            Text(text = "Operator Mode")
//        }
//
//        // Administrator Mode Button
//        Button(
//            onClick = { onAdminModeSelected() },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text(text = "Administrator Mode")
//        }
//    }
//}
//
//@Composable
//fun AdminModeDialog(onDismiss: () -> Unit, onModeSelected: (Boolean) -> Unit, isOnlineEnabled: Boolean) {
//    AlertDialog(
//        onDismissRequest = { onDismiss() },
//        title = { Text("Select Mode") },
//        text = { Text("Choose between online or offline mode for Admin Mode.") },
//        confirmButton = {
//            Button(
//                onClick = { onModeSelected(true) },
//                enabled = isOnlineEnabled  // Enable the button only if the network is available
//            ) {
//                Text("Online")
//            }
//        },
//        dismissButton = {
//            Button(onClick = {
//                onModeSelected(false)  // Offline mode selected
//            }) {
//                Text("Offline")
//            }
//        }
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun PreviewModeSelectionScreen() {
//    MB3Theme {
//        ModeSelectionScreen({}, {})
//    }
//}


//package com.tocho.mb3
//
//import android.content.Intent
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import com.tocho.mb3.ui.theme.MB3Theme
//import com.tocho.mb3.R
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            MB3Theme {
//                var showAdminDialog by remember { mutableStateOf(false) }
//
//                // Show mode selection screen
//                ModeSelectionScreen(
//                    onOperatorModeSelected = {
//                        val intent = Intent(this, AdminModeActivity::class.java)
//                        intent.putExtra("isOnline", true)  // Or false based on the mode
//                        startActivity(intent)
//                    },
//                    onAdminModeSelected = {
//                        showAdminDialog = true  // Trigger dialog for Admin Mode selection
//                    }
//                )
//
//                // Show online/offline dialog when showAdminDialog is true
//                if (showAdminDialog) {
//                    AdminModeDialog(
//                        onDismiss = { showAdminDialog = false },
//                        onModeSelected = { isOnline ->
//                            showAdminDialog = false
//                            val intent = Intent(this, AdminModeActivity::class.java)
//                            intent.putExtra("isOnline", isOnline)
//                            startActivity(intent)
//                        }
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun ModeSelectionScreen(
//    onOperatorModeSelected: () -> Unit,
//    onAdminModeSelected: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        // Display the logo above the buttons
//        Image(
//            painter = painterResource(id = R.drawable.mb3logotemp), // Replace with your actual image name
//            contentDescription = "App Logo",
//            modifier = Modifier
//                .size(150.dp) // Adjust the size of the logo
//                .padding(bottom = 32.dp)
//        )
//
//        // Operator Mode Button
//        Button(
//            onClick = { onOperatorModeSelected() },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 16.dp)
//        ) {
//            Text(text = "Operator Mode")
//        }
//
//        // Administrator Mode Button
//        Button(
//            onClick = { onAdminModeSelected() },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text(text = "Administrator Mode")
//        }
//    }
//}
//
//@Composable
//fun AdminModeDialog(onDismiss: () -> Unit, onModeSelected: (Boolean) -> Unit) {
//    AlertDialog(
//        onDismissRequest = { onDismiss() },
//        title = { Text("Select Mode") },
//        text = { Text("Choose between online or offline mode for Admin Mode.") },
//        confirmButton = {
//            Button(onClick = {
//                onModeSelected(true)  // Online mode selected
//            }) {
//                Text("Online")
//            }
//        },
//        dismissButton = {
//            Button(onClick = {
//                onModeSelected(false)  // Offline mode selected
//            }) {
//                Text("Offline")
//            }
//        }
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun PreviewModeSelectionScreen() {
//    MB3Theme {
//        ModeSelectionScreen({}, {})
//    }
//}