import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tocho.mb3.MarkingItem
import com.tocho.mb3.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image

// Define the MainScreen to handle navigation between GridComponent and FileSelectionPage
@Composable
fun MainScreen() {
    // State to manage the current screen
    var currentScreen by remember { mutableStateOf("Grid") }

    when (currentScreen) {
        "Grid" -> GridComponent(
            textFields = mutableMapOf(),  // Pass an actual state or mock data here
            onCellClick = {},
            onTextClick = { _, _ -> },
            sendTestDataToMB3 = {},
            sendTextFieldsToMB3 = {},
            gridColor = Color.LightGray,
            lineThickness = 1f,
            onFileSelectionClick = { currentScreen = "FileSelection" } // Navigate to FileSelectionPage
        )
        "FileSelection" -> FileSelectionPage(
            onBack = { currentScreen = "Grid" } // Navigate back to GridComponent
        )
    }
}

// Helper function at the top of the file, above the composable definitions
private fun calculateGridCoordinates(
    tapOffset: Offset,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    cellSizePx: Float,
    columns: Int,
    rows: Int
): Pair<Int, Int>? {
    // Adjust tap offsets by removing panning and scaling
    val adjustedX = (tapOffset.x - offsetX) / scale
    val adjustedY = (tapOffset.y - offsetY) / scale

    println("Adjusted Tap Coordinates: X: $adjustedX, Y: $adjustedY")

    // Calculate column and row based on cell size
    val col = (adjustedX / cellSizePx).toInt().coerceIn(0, columns - 1)
    val row = (adjustedY / cellSizePx).toInt().coerceIn(0, rows - 1)

    println("Calculated Col: $col, Row: $row for Tap at X: ${tapOffset.x}, Y: ${tapOffset.y}")

    return if (col in 0 until columns && row in 0 until rows) {
        col to row
    } else null
}

@Composable
fun GridComponent(
    textFields: MutableMap<Int, MarkingItem>,
    onCellClick: (Int) -> Unit,
    onTextClick: (Int, MarkingItem) -> Unit,
    sendTestDataToMB3: () -> Unit,
    sendTextFieldsToMB3: (MutableMap<Int, MarkingItem>) -> Unit,
    gridColor: Color = Color.LightGray,
    lineThickness: Float = 1f,

    onFileSelectionClick: () -> Unit // New callback for file selection navigation
) {
    // Predefined grid sizes
    val gridSizes = listOf(
        "80 x 20" to (80 to 20),
        "33 x 15" to (33 to 15),
        "20 x 15" to (20 to 15),
        "15 x 15" to (15 to 15),
        "10 x 10" to (10 to 10)
    )

    // State for managing the current selected grid size
    var selectedGridSize by remember { mutableStateOf(gridSizes[0].first) }
    var expanded by remember { mutableStateOf(false) }
    val (columns, rows) = gridSizes.first { it.first == selectedGridSize }.second

    // State for zoom and pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
//    val density = LocalDensity.current

    // State to track whether text-adding is enabled
    var isTextAddingEnabled by remember { mutableStateOf(true) }
    var isQRCodeAddingEnabled by remember { mutableStateOf(true) }
    var isDMAddingEnabled by remember { mutableStateOf(true) }
    var isSerialAddingEnabled by remember { mutableStateOf(true) }

    var isCalendarAddingEnabled by remember { mutableStateOf(true) }

//    // State to track recomposition manually
//    var refreshKey by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
                verticalArrangement = Arrangement.Top
    ) {
        // Top row with buttons and dropdown
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            ToggleTextAddingImageButton(
                isTextAddingEnabled = isTextAddingEnabled,
                onToggle = { isTextAddingEnabled = !isTextAddingEnabled }
            )

            ClearDataImageButton(
                onClear = {
                    textFields.clear() // Clear all text fields
                },
                onToggle = { isTextAddingEnabled = !isTextAddingEnabled } // Call ToggleTextAddingImageButton's toggle logic
            )

            SendDataButton(
                onClick = { sendTestDataToMB3() },
                imageRes = R.drawable.send_brown,  // Replace with actual send icon
                description = "Send Test Data"
            )

            /*
            // Add the "Clear All" button
            Button(
                onClick = {
                    textFields.clear()  // Clear all text fields in the grid
                },
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text("Clear All")
            }
            */

            SendDataButton(
                onClick = { sendTextFieldsToMB3(textFields) },
                imageRes = R.drawable.send_all_brown,  // Replace with actual sendT icon
                description = "Send Text Fields"
            )

//            Button(onClick = { sendTestDataToMB3() }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("Send")
//            }
//
//            Button(onClick = { sendTextFieldsToMB3(textFields) }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("SendT")
//            }

            // Replaced dropdown menu button with a toggleable image button
            Box {
                ToggleImageButton(isExpanded = expanded, onClick = { expanded = !expanded })

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    gridSizes.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size.first) },
                            onClick = {
                                selectedGridSize = size.first
                                expanded = false
                            }
                        )
                    }
                }
            }

            ZoomInButton(onClick = { scale += 0.1f })
            ZoomOutButton(onClick = { scale -= 0.1f })
//            Button(onClick = { scale += 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom In") }
//            Button(onClick = { scale -= 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom Out") }

            // Add File Selection Button
            Button(
                onClick = { onFileSelectionClick() },
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text("File Selection")
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(top = 0.dp, start = 0.dp),
                verticalArrangement = Arrangement.Top
                //verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToggleQRAddingImageButton(
                    isQRCodeAddingEnabled = isQRCodeAddingEnabled,
                    onToggle = { isQRCodeAddingEnabled = !isQRCodeAddingEnabled }
                )
                ToggleDMAddingImageButton(
                    isDMAddingEnabled = isDMAddingEnabled,
                    onToggle = { isDMAddingEnabled = !isDMAddingEnabled }
                )
                ToggleSerialAddingImageButton(
                    isSerialAddingEnabled = isSerialAddingEnabled,
                    onToggle = { isSerialAddingEnabled = !isSerialAddingEnabled }
                )
                ToggleCalendarAddingImageButton(
                    isCalendarAddingEnabled = isCalendarAddingEnabled,
                    onToggle = { isCalendarAddingEnabled = !isCalendarAddingEnabled }
                )
            }

            // Grid Layout
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale *= zoom
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
            ) {
                val density = LocalDensity.current
                // Calculate a single size for square cells
                val cellSizePx = with(density) {
                    minOf(maxWidth / columns, maxHeight / rows).toPx() * scale
                }

                // Calculate cell size based on the scale
//                val cellSizePx = with(density) { minOf(maxWidth / columns, maxHeight / rows).toPx() * scale }

                // Tap Gesture to handle cell selection based on tap position
                val tapGestureModifier = Modifier.pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        calculateGridCoordinates(tapOffset, offsetX, offsetY, scale, cellSizePx, columns, rows)?.let { (col, row) ->
                            val index = row * columns + col
                            if (isTextAddingEnabled && !textFields.containsKey(index)) {
                                onCellClick(index)
                                println("Tapped on cell: ($col, $row), Index: $index")
                            }
                        } ?: println("Tap outside grid bounds")
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY.dp)
                        .then(tapGestureModifier)
                ) {
                    val gridWidth = cellSizePx * columns
                    val gridHeight = cellSizePx * rows

                    for (i in 0..columns) {
                        val startX = i * cellSizePx
                        drawLine(
                            color = gridColor,
                            start = Offset(startX, 0f),
                            end = Offset(startX, gridHeight),
                            strokeWidth = lineThickness
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "$i",
                            startX + cellSizePx / 2,
                            -10f,
                            android.graphics.Paint().apply {
                                textSize = 20f * scale
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }

                    for (i in 0..rows) {
                        val startY = i * cellSizePx
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, startY),
                            end = Offset(gridWidth, startY),
                            strokeWidth = lineThickness
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "$i",
                            -10f,
                            startY + cellSizePx / 2,
                            android.graphics.Paint().apply {
                                textSize = 20f * scale
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )
                    }
                }
                // Render Text Fields with consistent coordinate calculation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY.dp)
                ) {
                    for ((index, textField) in textFields) {
//                    for ((index, textField) in textFieldsState.value) {
                        val row = index / columns
                        val col = index % columns

                        // Get the exact on-screen position using calculateGridCoordinates
                        val onScreenPosition = calculateGridCoordinates(
                            Offset(col * cellSizePx, row * cellSizePx),
                            offsetX,
                            offsetY,
                            scale,
                            cellSizePx,
                            columns,
                            rows
                        )

                        if (onScreenPosition != null) {
                            val (renderCol, renderRow) = onScreenPosition
//                            val textX = renderCol * cellSizePx
//                            val textY = renderRow * cellSizePx
                            val textX = (renderCol * cellSizePx * scale) + offsetX
                            val textY = (renderRow * cellSizePx * scale) + offsetY

                            println("Rendering Text at Col: $renderCol, Row: $renderRow with X: $textX, Y: $textY")  // Debug output

                            Box(
                                modifier = Modifier
                                    .offset(x = with(density) { textX.toDp() }, y = with(density) { textY.toDp() })
                                    .clickable {
                                        onTextClick(index, textField)
                                    },
                                contentAlignment = Alignment.TopStart
                            ) {
                                if (textField.text.isNotBlank()) {
                                    val fontSize = (textField.height * cellSizePx / density.density).sp
                                    Text(
                                        text = textField.text,
                                        fontSize = fontSize,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier
                                            .wrapContentWidth(Alignment.Start)
                                            .wrapContentHeight(Alignment.Top)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SendDataButton(
    onClick: () -> Unit,
    imageRes: Int,
    description: String
) {
    // Animation state for button press
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.1f else 1f,
        animationSpec = tween(durationMillis = 150)
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .border(2.dp, Color.LightGray)
            .graphicsLayer(scaleX = scale, scaleY = scale)  // Apply scale animation
            .clickable {
                onClick()  // Call the action
                isPressed = true
                coroutineScope.launch {
                    delay(150)  // Reset animation after delay
                    isPressed = false
                }
            }
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = description,
            contentScale = ContentScale.FillBounds,  // Ensures it fills the entire space
            modifier = Modifier.fillMaxSize()  // Make sure it fills the Box fully
        )
    }
}


@Composable
fun ToggleTextAddingImageButton(
    isTextAddingEnabled: Boolean,
    onToggle: () -> Unit
) {
    // Image resource based on the toggle state
    val imageRes = if (isTextAddingEnabled) {
        R.drawable.text_brown  // Image for "Enable Add Text" state
    } else {
        R.drawable.text_white  // Image for "Disable Add Text" state
    }

    // Animated scaling for a smooth transition when tapped
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Use a Box and Image to have full control over appearance
    Box(
        modifier = Modifier
            .size(48.dp)  // Size of the button
            .border(2.dp, Color.LightGray)
            .clickable {
                onToggle()
                isPressed = true
                coroutineScope.launch {
                    delay(150)
                    isPressed = false
                }
            }
            .graphicsLayer(
                scaleX = if (isPressed) 1.1f else 1f,
                scaleY = if (isPressed) 1.1f else 1f
            )
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text",
            contentScale = ContentScale.FillBounds,  // Ensures it fills the entire space
            modifier = Modifier.fillMaxSize()  // Make sure it fills the Box fully
        )
    }
}

@Composable
fun ToggleImageButton(isExpanded: Boolean, onClick: () -> Unit) {
    val imageRes = if (isExpanded) {
        R.drawable.machine_model_white
    } else {
        R.drawable.machine_model_brown
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .border(2.dp, Color.LightGray)  // Light grey border around the box
            .clickable { onClick() }  // Trigger the click action
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Dropdown icon",
            modifier = Modifier.fillMaxSize(),  // Slightly smaller to fit within the border
            contentScale = ContentScale.FillBounds // Fit without cropping
        )
    }
}

@Composable
fun ZoomInButton(onClick: () -> Unit) {
    var isZoomingIn by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Animation for scaling the icon when clicked
    val scale by animateFloatAsState(
        targetValue = if (isZoomingIn) 1.2f else 1f,  // Scale up slightly when clicked
        animationSpec = tween(durationMillis = 150)
    )

    // Determine the image resource based on the zoom state
    val imageRes = if (isZoomingIn) {
        R.drawable.zoom_in_white  // Active image when pressed
    } else {
        R.drawable.zoom_in_brown  // Default image
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .border(2.dp, Color.LightGray)  // Light grey border around the box
            .graphicsLayer(scaleX = scale, scaleY = scale)  // Apply scale animation
            .clickable {
                isZoomingIn = true
                onClick()

                // Reset the zoom state after a short delay
                coroutineScope.launch {
                    delay(250)
                    isZoomingIn = false
                }
            }
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Zoom In",
            modifier = Modifier.fillMaxSize(),  // Slightly smaller to fit within the border
            contentScale = ContentScale.FillBounds // Fit without cropping
        )
    }
}

@Composable
fun ZoomOutButton(onClick: () -> Unit) {
    var isZoomingOut by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Animation for scaling the icon when clicked
    val scale by animateFloatAsState(
        targetValue = if (isZoomingOut) 1.2f else 1f,  // Scale up slightly when clicked
        animationSpec = tween(durationMillis = 150)
    )

    // Determine the image resource based on the zoom state
    val imageRes = if (isZoomingOut) {
        R.drawable.zoom_out_white  // Active image when pressed
    } else {
        R.drawable.zoom_out_brown  // Default image
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .border(2.dp, Color.LightGray)  // Light grey border around the box
            .graphicsLayer(scaleX = scale, scaleY = scale)  // Apply scale animation
            .clickable {
                isZoomingOut = true
                onClick()

                // Reset the zoom state after a short delay
                coroutineScope.launch {
                    delay(250)
                    isZoomingOut = false
                }
            }
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Zoom Out",
            modifier = Modifier.fillMaxSize(),  // Slightly smaller to fit within the border
            contentScale = ContentScale.FillBounds // Fit without cropping
        )
    }
}

@Composable
fun ToggleQRAddingImageButton(
    isQRCodeAddingEnabled: Boolean,
    onToggle: () -> Unit
) {
    // Image resource based on the toggle state
    val imageRes = if (isQRCodeAddingEnabled) {
        R.drawable.qr_code_brown  // Image for "Enable Add Text" state
    } else {
        R.drawable.qr_code_white  // Image for "Disable Add Text" state
    }

    // Animated scaling for a smooth transition when tapped
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Use a Box and Image to have full control over appearance
    Box(
        modifier = Modifier
            .size(48.dp)  // Size of the button
            .border(2.dp, Color.LightGray)
            .clickable {
                onToggle()
                isPressed = true
                coroutineScope.launch {
                    delay(150)
                    isPressed = false
                }
            }
            .graphicsLayer(
                scaleX = if (isPressed) 1.1f else 1f,
                scaleY = if (isPressed) 1.1f else 1f
            )
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = if (isQRCodeAddingEnabled) "Disable QR Code" else "Enable QR Code",
            contentScale = ContentScale.FillBounds,  // Ensures it fills the entire space
            modifier = Modifier.fillMaxSize()  // Make sure it fills the Box fully
        )
    }
}

@Composable
fun ToggleDMAddingImageButton(
    isDMAddingEnabled: Boolean,
    onToggle: () -> Unit
) {
    // Image resource based on the toggle state
    val imageRes = if (isDMAddingEnabled) {
        R.drawable.data_matrix_brown  // Image for "Enable Add Text" state
    } else {
        R.drawable.data_matrix_white  // Image for "Disable Add Text" state
    }

    // Animated scaling for a smooth transition when tapped
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Use a Box and Image to have full control over appearance
    Box(
        modifier = Modifier
            .size(48.dp)  // Size of the button
            .border(2.dp, Color.LightGray)
            .clickable {
                onToggle()
                isPressed = true
                coroutineScope.launch {
                    delay(150)
                    isPressed = false
                }
            }
            .graphicsLayer(
                scaleX = if (isPressed) 1.1f else 1f,
                scaleY = if (isPressed) 1.1f else 1f
            )
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = if (isDMAddingEnabled) "Disable Data Matrix" else "Enable Data Matrix",
            contentScale = ContentScale.FillBounds,  // Ensures it fills the entire space
            modifier = Modifier.fillMaxSize()  // Make sure it fills the Box fully
        )
    }
}

@Composable
fun ToggleSerialAddingImageButton(
    isSerialAddingEnabled: Boolean,
    onToggle: () -> Unit
) {
    // Image resource based on the toggle state
    val imageRes = if (isSerialAddingEnabled) {
        R.drawable.serial_brown  // Image for "Enable Add Text" state
    } else {
        R.drawable.serial_white // Image for "Disable Add Text" state
    }

    // Animated scaling for a smooth transition when tapped
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Use a Box and Image to have full control over appearance
    Box(
        modifier = Modifier
            .size(48.dp)  // Size of the button
            .border(2.dp, Color.LightGray)
            .clickable {
                onToggle()
                isPressed = true
                coroutineScope.launch {
                    delay(150)
                    isPressed = false
                }
            }
            .graphicsLayer(
                scaleX = if (isPressed) 1.1f else 1f,
                scaleY = if (isPressed) 1.1f else 1f
            )
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = if (isSerialAddingEnabled) "Disable Serial" else "Enable Serial",
            contentScale = ContentScale.FillBounds,  // Ensures it fills the entire space
            modifier = Modifier.fillMaxSize()  // Make sure it fills the Box fully
        )
    }
}

@Composable
fun ToggleCalendarAddingImageButton(
    isCalendarAddingEnabled: Boolean,
    onToggle: () -> Unit
) {
    // Image resource based on the toggle state
    val imageRes = if (isCalendarAddingEnabled) {
        R.drawable.calendar_brown  // Image for "Enable Add Text" state
    } else {
        R.drawable.calendar_white // Image for "Disable Add Text" state
    }

    // Animated scaling for a smooth transition when tapped
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Use a Box and Image to have full control over appearance
    Box(
        modifier = Modifier
            .size(48.dp)  // Size of the button
            .border(2.dp, Color.LightGray)
            .clickable {
                onToggle()
                isPressed = true
                coroutineScope.launch {
                    delay(150)
                    isPressed = false
                }
            }
            .graphicsLayer(
                scaleX = if (isPressed) 1.1f else 1f,
                scaleY = if (isPressed) 1.1f else 1f
            )
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = if (isCalendarAddingEnabled) "Disable Calendar" else "Enable Calendar",
            contentScale = ContentScale.FillBounds,  // Ensures it fills the entire space
            modifier = Modifier.fillMaxSize()  // Make sure it fills the Box fully
        )
    }
}

@Composable
fun ClearDataImageButton(
    onClear: () -> Unit,
    onToggle: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(48.dp) // Size of the button
            .border(2.dp, Color.LightGray)
            .clickable {
                coroutineScope.launch {
                    onToggle() // First call to ToggleTextAddingImageButton's logic
                    delay(100) // Short delay to simulate two separate calls
                    onToggle() // Second call to ToggleTextAddingImageButton's logic
                    onClear()  // Clear the grid
                }
                isPressed = true
                coroutineScope.launch {
                    delay(150)
                    isPressed = false
                }
            }
            .graphicsLayer(
                scaleX = if (isPressed) 1.1f else 1f,
                scaleY = if (isPressed) 1.1f else 1f
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.clear_brown), // Replace with actual image
            contentDescription = "Clear Data",
            contentScale = ContentScale.FillBounds, // Ensures it fills the entire space
            modifier = Modifier.fillMaxSize() // Make sure it fills the Box fully
        )
    }
}

// Define the FileSelectionPage composable
@Composable
fun FileSelectionPage(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("File Selection Page")

        Button(
            onClick = { onBack() }, // Navigate back to GridComponent
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Back to Grid")
        }
    }
}
//
//
//@Composable
//fun ClearDataImageButton(onClear: () -> Unit) {
//    var isPressed by remember { mutableStateOf(false) }
//    val coroutineScope = rememberCoroutineScope()
//
//    Box(
//        modifier = Modifier
//            .size(48.dp)
//            .border(2.dp, Color.LightGray)
//            .clickable {
//                onClear()
//                isPressed = true
//                coroutineScope.launch {
//                    delay(150)
//                    isPressed = false
//                }
//            }
//            .graphicsLayer(
//                scaleX = if (isPressed) 1.1f else 1f,
//                scaleY = if (isPressed) 1.1f else 1f
//            )
//    ) {
//        Image(
//            painter = painterResource(id = R.drawable.clear_brown),
//            contentDescription = "Clear Grid",
//            contentScale = ContentScale.FillBounds,
//            modifier = Modifier.fillMaxSize()
//        )
//    }
//}

/*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tocho.mb3.MarkingItem

@Composable
fun GridComponent(
    textFields: MutableMap<Int, MarkingItem>,
    onCellClick: (Int) -> Unit,
    onTextClick: (Int, MarkingItem) -> Unit,
    sendTestDataToMB3: () -> Unit,
    sendTextFieldsToMB3: (MutableMap<Int, MarkingItem>) -> Unit,
    gridColor: Color = Color.LightGray,
    lineThickness: Float = 1f
) {
    // Predefined grid sizes
    val gridSizes = listOf(
        "80 x 20" to (80 to 20),
        "33 x 15" to (33 to 15),
        "20 x 15" to (20 to 15),
        "15 x 15" to (15 to 15),
        "10 x 10" to (10 to 10)
    )

    // State for managing the current selected grid size
    var selectedGridSize by remember { mutableStateOf(gridSizes[0].first) }
    var expanded by remember { mutableStateOf(false) }
    val (columns, rows) = gridSizes.first { it.first == selectedGridSize }.second

    // State for zoom and pan
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // State to track whether text-adding is enabled
    var isTextAddingEnabled by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().padding(0.dp)) {
        // Top row with buttons and dropdown
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            Button(onClick = { isTextAddingEnabled = !isTextAddingEnabled }) {
                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
            }

            Button(onClick = { sendTestDataToMB3() }, modifier = Modifier.padding(start = 4.dp)) {
                Text("Send")
            }

            Button(onClick = { sendTextFieldsToMB3(textFields) }, modifier = Modifier.padding(start = 4.dp)) {
                Text("SendT")
            }

            // Button for selecting grid size with DropdownMenu
            Box {
                Button(onClick = { expanded = true }) {
                    Text("Grid Size: $selectedGridSize")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    gridSizes.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size.first) },
                            onClick = {
                                selectedGridSize = size.first
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(onClick = { scale += 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom In") }
            Button(onClick = { scale -= 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom Out") }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(top = 4.dp, start = 0.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(onClick = { /* Action A */ }) { Text("B") }
                Button(onClick = { /* Action B */ }) { Text("C") }
                Button(onClick = { /* Action C */ }) { Text("D") }
                Button(onClick = { /* Action D */ }) { Text("E") }
            }

            // Grid Layout
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale *= zoom
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
            ) {
                val density = LocalDensity.current
                // Calculate a single size for square cells
                val cellSizePx = with(density) {
                    minOf(maxWidth / columns, maxHeight / rows).toPx() * scale
                }

                // Tap Gesture to handle cell selection based on tap position
                val tapGestureModifier = Modifier.pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        // Convert tap offset to grid coordinates considering scale and offset
                        val adjustedX = (tapOffset.x - offsetX) / scale
                        val adjustedY = (tapOffset.y - offsetY) / scale

                        val col = (adjustedX / cellSizePx).toInt()
                        val row = (adjustedY / cellSizePx).toInt()

                        println("Tap Offset: (${tapOffset.x}, ${tapOffset.y})")
                        println("Adjusted X, Y: ($adjustedX, $adjustedY)")
                        println("Calculated Col, Row: ($col, $row)")

                        if (col in 0 until columns && row in 0 until rows) {
                            val index = row * columns + col
                            if (isTextAddingEnabled && !textFields.containsKey(index)) {
                                onCellClick(index)
                                println("Tapped on cell: ($col, $row), Index: $index")
                            }
                        } else {
                            println("Tap outside grid bounds")
                        }
                    }
                }

                // Canvas for drawing numbers and grid together
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY.dp)
                        .then(tapGestureModifier)
                ) {
                    val gridWidth = cellSizePx * columns
                    val gridHeight = cellSizePx * rows

                    for (i in 0..columns) {
                        val startX = i * cellSizePx
                        drawLine(
                            color = gridColor,
                            start = Offset(startX, 0f),
                            end = Offset(startX, gridHeight),
                            strokeWidth = lineThickness
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "$i",
                            startX + cellSizePx / 2,
                            -10f,
                            android.graphics.Paint().apply {
                                textSize = 20f * scale
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }

                    for (i in 0..rows) {
                        val startY = i * cellSizePx
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, startY),
                            end = Offset(gridWidth, startY),
                            strokeWidth = lineThickness
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "$i",
                            -10f,
                            startY + cellSizePx / 2,
                            android.graphics.Paint().apply {
                                textSize = 20f * scale
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )
                    }
                }

                // Text fields overlay and click handlers
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY.dp)
                ) {
                    for (index in 0 until rows * columns) {
                        val col = index % columns
                        val row = index / columns

                        val textX = with(density) { (col * cellSizePx).toDp() }
                        val textY = with(density) { (row * cellSizePx).toDp() }

                        Box(
                            modifier = Modifier
                                .offset(x = textX, y = textY)
                                .clickable {
                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
                                        onCellClick(index)
                                    }
                                },
                            contentAlignment = Alignment.TopStart
                        ) {
                            textFields[index]?.let { textField ->
                                if (textField.text.isNotBlank()) {
                                    val fontSize = (textField.height * cellSizePx / density.density).sp
                                    Text(
                                        text = textField.text,
                                        fontSize = fontSize,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier
                                            .wrapContentWidth(Alignment.Start)
                                            .wrapContentHeight(Alignment.Top)
                                            .clickable { onTextClick(index, textField) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
*/


/*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tocho.mb3.MarkingItem

@Composable
fun GridComponent(
    textFields: MutableMap<Int, MarkingItem>,
    onCellClick: (Int) -> Unit,
    onTextClick: (Int, MarkingItem) -> Unit,
    sendTestDataToMB3: () -> Unit,
    sendTextFieldsToMB3: (MutableMap<Int, MarkingItem>) -> Unit,
    gridColor: Color = Color.LightGray,
    lineThickness: Float = 1f
) {
    // Predefined grid sizes
    val gridSizes = listOf(
        "80 x 20" to (80 to 20),
        "33 x 15" to (33 to 15),
        "20 x 15" to (20 to 15),
        "15 x 15" to (15 to 15),
        "10 x 10" to (10 to 10)
    )

    // State for managing the current selected grid size
    var selectedGridSize by remember { mutableStateOf(gridSizes[0].first) }
    var expanded by remember { mutableStateOf(false) }
    val (columns, rows) = gridSizes.first { it.first == selectedGridSize }.second

    // State for zoom and pan
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // State to track whether text-adding is enabled
    var isTextAddingEnabled by remember { mutableStateOf(true) }

    // Gesture detector for panning and zooming
    val panZoomModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            scale *= zoom
            offsetX += pan.x
            offsetY += pan.y
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(0.dp)) {
        // Top row with buttons and dropdown
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            Button(onClick = { isTextAddingEnabled = !isTextAddingEnabled }) {
                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
            }

            Button(onClick = { sendTestDataToMB3() }, modifier = Modifier.padding(start = 4.dp)) {
                Text("Send")
            }

            Button(onClick = { sendTextFieldsToMB3(textFields) }, modifier = Modifier.padding(start = 4.dp)) {
                Text("SendT")
            }

            // Button for selecting grid size with DropdownMenu
            Box {
                Button(onClick = { expanded = true }) {
                    Text("Grid Size: $selectedGridSize")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    gridSizes.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size.first) },
                            onClick = {
                                selectedGridSize = size.first
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(onClick = { scale += 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom In") }
            Button(onClick = { scale -= 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom Out") }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(top = 4.dp, start = 0.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(onClick = { /* Action A */ }) { Text("B") }
                Button(onClick = { /* Action B */ }) { Text("C") }
                Button(onClick = { /* Action C */ }) { Text("D") }
                Button(onClick = { /* Action D */ }) { Text("E") }
            }

            // Grid Layout
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .then(panZoomModifier)  // Add pan and zoom modifier here
            ) {
                val density = LocalDensity.current  // Use LocalDensity to convert Dp to Px
                // Calculate a single size for square cells
                val cellSizePx = with(density) {
                    minOf(maxWidth / columns, maxHeight / rows).toPx() * scale
                }

                // Ensure both width and height are square by using `cellSizePx` for both dimensions
                val cellWidthPx = cellSizePx
                val cellHeightPx = cellSizePx

                // Canvas for drawing numbers and grid together
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY.dp)  // Apply the same offset as the grid
                ) {
                    val gridWidth = cellWidthPx * columns
                    val gridHeight = cellHeightPx * rows

                    // Draw vertical grid lines and top row numbers
                    for (i in 0..columns) {
                        val startX = i * cellWidthPx
                        drawLine(
                            color = gridColor,
                            start = Offset(startX, 0f),
                            end = Offset(startX, gridHeight),
                            strokeWidth = lineThickness
                        )
                        // Draw top row numbers aligned with vertical lines
                        drawContext.canvas.nativeCanvas.drawText(
                            "$i",
                            startX + cellWidthPx / 2,  // Center horizontally within each cell
                            -cellHeightPx * 0.3f,      // Position slightly above the top boundary
                            android.graphics.Paint().apply {
                                textSize = 20f * scale
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }

                    // Draw horizontal grid lines and left column numbers
                    for (i in 0..rows) {
                        val startY = i * cellHeightPx
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, startY),
                            end = Offset(gridWidth, startY),
                            strokeWidth = lineThickness
                        )
                        // Draw left column numbers aligned with horizontal lines
                        drawContext.canvas.nativeCanvas.drawText(
                            "$i",
                            -cellWidthPx * 0.3f,          // Position slightly to the left of the left boundary
                            startY + cellHeightPx / 2,    // Center vertically within each cell
                            android.graphics.Paint().apply {
                                textSize = 20f * scale
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )
                    }
                }

                // Text fields overlay and click handlers
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY.dp)
                ) {
                    for (index in 0 until rows * columns) {
                        val col = index % columns
                        val row = index / columns

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = with(density) { (col * cellSizePx).toDp() },
                                    y = with(density) { (row * cellSizePx).toDp() }
                                )
                                .clickable {
                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
                                        onCellClick(index)
                                    }
                                },
                            contentAlignment = Alignment.TopStart
                        ) {
                            textFields[index]?.let { textField ->
                                if (textField.text.isNotBlank()) {
                                    // Calculate the font size based on the grid's cell height and the number of rows the text should cover
                                    val fontSize = (textField.height * cellSizePx / density.density).sp

                                    // Ensure the text is left-aligned and flows left to right
                                    Text(
                                        text = textField.text,
                                        fontSize = fontSize,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier
                                            .wrapContentWidth(Alignment.Start)
                                            .wrapContentHeight(Alignment.Top)
                                            .clickable { onTextClick(index, textField) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.gestures.detectTransformGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.nativeCanvas
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.tocho.mb3.MarkingItem
//
//@Composable
//fun GridComponent(
//    textFields: MutableMap<Int, MarkingItem>,
//    onCellClick: (Int) -> Unit,
//    onTextClick: (Int, MarkingItem) -> Unit,
//    sendTestDataToMB3: () -> Unit,
//    sendTextFieldsToMB3: (MutableMap<Int, MarkingItem>) -> Unit,
//    gridColor: Color = Color.LightGray,
//    lineThickness: Float = 1f
//) {
//    // Predefined grid sizes
//    val gridSizes = listOf(
//        "80 x 20" to (80 to 20),
//        "33 x 15" to (33 to 15),
//        "20 x 15" to (20 to 15),
//        "15 x 15" to (15 to 15),
//        "10 x 10" to (10 to 10)
//    )
//
//    // State for managing the current selected grid size
//    var selectedGridSize by remember { mutableStateOf(gridSizes[0].first) }
//    var expanded by remember { mutableStateOf(false) }
//    val (columns, rows) = gridSizes.first { it.first == selectedGridSize }.second
//
//    // State for zoom and pan
//    var scale by remember { mutableStateOf(1f) }
//    var offsetX by remember { mutableStateOf(0f) }
//    var offsetY by remember { mutableStateOf(0f) }
//
//    // State to track whether text-adding is enabled
//    var isTextAddingEnabled by remember { mutableStateOf(true) }
//
//    // Gesture detector for panning and zooming
//    val panZoomModifier = Modifier.pointerInput(Unit) {
//        detectTransformGestures { _, pan, zoom, _ ->
//            scale *= zoom
//            offsetX += pan.x
//            offsetY += pan.y
//        }
//    }
//
//    Column(modifier = Modifier.fillMaxSize().padding(0.dp)) {
//        // Top row with buttons and dropdown
//        Row(
//            verticalAlignment = Alignment.Top,
//            horizontalArrangement = Arrangement.Start
//        ) {
//            Button(onClick = { isTextAddingEnabled = !isTextAddingEnabled }) {
//                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
//            }
//
//            Button(onClick = { sendTestDataToMB3() }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("Send")
//            }
//
//            Button(onClick = { sendTextFieldsToMB3(textFields) }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("SendT")
//            }
//
//            // Button for selecting grid size with DropdownMenu
//            Box {
//                Button(onClick = { expanded = true }) {
//                    Text("Grid Size: $selectedGridSize")
//                }
//                DropdownMenu(
//                    expanded = expanded,
//                    onDismissRequest = { expanded = false }
//                ) {
//                    gridSizes.forEach { size ->
//                        DropdownMenuItem(
//                            text = { Text(size.first) },
//                            onClick = {
//                                selectedGridSize = size.first
//                                expanded = false
//                            }
//                        )
//                    }
//                }
//            }
//
//            Button(onClick = { scale += 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom In") }
//            Button(onClick = { scale -= 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom Out") }
//        }
//
//        Row(modifier = Modifier.fillMaxSize()) {
//            Column(
//                modifier = Modifier.padding(top = 4.dp, start = 0.dp),
//                verticalArrangement = Arrangement.spacedBy(4.dp)
//            ) {
//                Button(onClick = { /* Action A */ }) { Text("B") }
//                Button(onClick = { /* Action B */ }) { Text("C") }
//                Button(onClick = { /* Action C */ }) { Text("D") }
//                Button(onClick = { /* Action D */ }) { Text("E") }
//            }
//
//            // Grid Layout
//            BoxWithConstraints(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(16.dp)
//                    .then(panZoomModifier)  // Add pan and zoom modifier here
//            ) {
//                val density = LocalDensity.current  // Use LocalDensity to convert Dp to Px
//                // Calculate a single size for square cells
//                val cellSizePx = with(density) {
//                    minOf(maxWidth / columns, maxHeight / rows).toPx() * scale
//                }
//
//                // Ensure both width and height are square by using `cellSizePx` for both dimensions
//                val cellWidthPx = cellSizePx
//                val cellHeightPx = cellSizePx
//
//                // Canvas for drawing numbers and grid together
//                Canvas(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .offset(x = offsetX.dp, y = offsetY.dp)  // Apply the same offset as the grid
//                ) {
//                    val gridWidth = cellWidthPx * columns
//                    val gridHeight = cellHeightPx * rows
//
//                    // Calculate a slight offset for positioning above and to the left of the grid boundaries
//                    val topBoundaryOffset = -cellHeightPx * 0.1f  // Adjust to place numbers just above the grid
//                    val leftBoundaryOffset = -cellWidthPx * 0.1f  // Adjust to place numbers just to the left of the grid
//
//                    // Draw top row numbers (horizontal numbers above the grid)
//                    for (i in 0..columns) {
//                        val startX = i * cellWidthPx
//                        drawContext.canvas.nativeCanvas.drawText(
//                            "$i",
//                            startX + cellWidthPx / 2,            // Center horizontally within each cell
//                            topBoundaryOffset,                   // Position slightly above the top boundary
//                            android.graphics.Paint().apply {
//                                textSize = 20f * scale           // Scale the text size with zoom
//                                textAlign = android.graphics.Paint.Align.CENTER
//                            }
//                        )
//                    }
//
//                    // Draw left column numbers (vertical numbers to the left of the grid)
//                    for (i in 0..rows) {
//                        val startY = i * cellHeightPx
//                        drawContext.canvas.nativeCanvas.drawText(
//                            "$i",
//                            leftBoundaryOffset,                  // Position slightly to the left of the left boundary
//                            startY + cellHeightPx / 2,           // Center vertically within each cell
//                            android.graphics.Paint().apply {
//                                textSize = 20f * scale           // Scale the text size with zoom
//                                textAlign = android.graphics.Paint.Align.RIGHT
//                            }
//                        )
//                    }
//
//                    // Draw vertical grid lines
//                    for (i in 0..columns) {
//                        val startX = i * cellWidthPx
//                        drawLine(
//                            color = gridColor,
//                            start = Offset(startX, 0f),
//                            end = Offset(startX, gridHeight),
//                            strokeWidth = lineThickness
//                        )
//                    }
//
//                    // Draw horizontal grid lines
//                    for (i in 0..rows) {
//                        val startY = i * cellHeightPx
//                        drawLine(
//                            color = gridColor,
//                            start = Offset(0f, startY),
//                            end = Offset(gridWidth, startY),
//                            strokeWidth = lineThickness
//                        )
//                    }
//                }
//
//                // Text fields overlay and click handlers
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .offset(x = offsetX.dp, y = offsetY.dp)
//                ) {
//                    for (index in 0 until rows * columns) {
//                        val col = index % columns
//                        val row = index / columns
//
//                        Box(
//                            modifier = Modifier
//                                .offset(
//                                    x = with(density) { (col * cellSizePx).toDp() },
//                                    y = with(density) { (row * cellSizePx).toDp() }
//                                )
//                                .clickable {
//                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
//                                        onCellClick(index)
//                                    }
//                                },
//                            contentAlignment = Alignment.TopStart
//                        ) {
//                            textFields[index]?.let { textField ->
//                                if (textField.text.isNotBlank()) {
//                                    // Calculate the font size based on the grid's cell height and the number of rows the text should cover
//                                    val fontSize = (textField.height * cellSizePx / density.density).sp
//
//                                    // Ensure the text is left-aligned and flows left to right
//                                    Text(
//                                        text = textField.text,
//                                        fontSize = fontSize,
//                                        textAlign = TextAlign.Start,
//                                        modifier = Modifier
//                                            .wrapContentWidth(Alignment.Start)
//                                            .wrapContentHeight(Alignment.Top)
//                                            .clickable { onTextClick(index, textField) }
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

/*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tocho.mb3.MarkingItem

@Composable
fun GridComponent(
    rows: Int = 20,
    columns: Int = 80,
    textFields: MutableMap<Int, MarkingItem>,
    onCellClick: (Int) -> Unit,
    onTextClick: (Int, MarkingItem) -> Unit,
    sendTestDataToMB3: () -> Unit,
    sendTextFieldsToMB3: (MutableMap<Int, MarkingItem>) -> Unit,
    gridColor: Color = Color.LightGray,
    lineThickness: Float = 1f
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isTextAddingEnabled by remember { mutableStateOf(true) }

    // Two-finger pan and zoom modifier
    val panZoomModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            scale *= zoom
            if (scale > 1.0f) {
                offsetX += pan.x
                offsetY += pan.y
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(0.dp)) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            // Button to toggle adding text to the grid
            Button(onClick = { isTextAddingEnabled = !isTextAddingEnabled }) {
                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
            }

            Button(onClick = { scale = 1f; offsetX = 0f; offsetY = 0f }) {
                Text("Reset")
            }

            Button(onClick = { sendTestDataToMB3() }, modifier = Modifier.padding(start = 4.dp)) {
                Text("Send")
            }

            Button(onClick = { sendTextFieldsToMB3(textFields) }, modifier = Modifier.padding(start = 4.dp)) {
                Text("SendT")
            }

            Button(onClick = { scale += 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom In") }
            Button(onClick = { scale -= 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom Out") }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(top = 4.dp, start = 0.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(onClick = { /* Action A */ }) { Text("B") }
                Button(onClick = { /* Action B */ }) { Text("C") }
                Button(onClick = { /* Action C */ }) { Text("D") }
                Button(onClick = { /* Action D */ }) { Text("E") }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .then(panZoomModifier)
            ) {
                val density = LocalDensity.current
                val cellWidthPx = with(density) { (maxWidth / columns).toPx() * scale }
                val cellHeightPx = with(density) { (maxHeight / rows).toPx() * scale }

                // Draw grid with Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY.dp)
                ) {
                    val gridWidth = cellWidthPx * columns
                    val gridHeight = cellHeightPx * rows

                    val adjustedOffsetX = (offsetX % cellWidthPx).coerceAtLeast(0f)
                    val adjustedOffsetY = (offsetY % cellHeightPx).coerceAtLeast(0f)

                    // Draw vertical numbers (top row numbers)
                    for (i in 0..columns) {
                        val startX = i * cellWidthPx + adjustedOffsetX
                        drawContext.canvas.nativeCanvas.drawText(
                            "$i",
                            startX + cellWidthPx / 2,
                            adjustedOffsetY - 20f,
                            android.graphics.Paint().apply {
                                textSize = 20f * scale
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }

                    // Draw horizontal numbers (left column numbers)
                    for (i in 0..rows) {
                        val startY = i * cellHeightPx + adjustedOffsetY
                        drawContext.canvas.nativeCanvas.drawText(
                            "$i",
                            adjustedOffsetX - 20f,
                            startY + cellHeightPx / 2,
                            android.graphics.Paint().apply {
                                textSize = 20f * scale
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )
                    }

                    // Draw vertical grid lines
                    for (i in 0..columns) {
                        val startX = i * cellWidthPx + adjustedOffsetX
                        drawLine(
                            color = gridColor,
                            start = Offset(startX, 0f),
                            end = Offset(startX, gridHeight),
                            strokeWidth = lineThickness
                        )
                    }

                    // Draw horizontal grid lines
                    for (i in 0..rows) {
                        val startY = i * cellHeightPx + adjustedOffsetY
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, startY),
                            end = Offset(gridWidth, startY),
                            strokeWidth = lineThickness
                        )
                    }
                }

                // Lazy grid for text fields
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(rows),  // Set the fixed number of rows
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY.dp)
                ) {
                    items(rows * columns) { index ->
                        val col = index % columns
                        val row = index / columns

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = with(density) { (col * cellWidthPx).toDp() },
                                    y = with(density) { (row * cellHeightPx).toDp() }
                                )
                                .clickable {
                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
                                        onCellClick(index)
                                    }
                                },
                            contentAlignment = Alignment.TopStart
                        ) {
                            textFields[index]?.let { textField ->
                                if (textField.text.isNotBlank()) {
                                    // Set font size based on rows
                                    val fontSize = (textField.height * cellHeightPx / density.density).sp

                                    // Ensure the text is left-aligned and flows left to right
                                    Text(
                                        text = textField.text,
                                        fontSize = fontSize,
                                        textAlign = TextAlign.Start,  // Left-to-right alignment
                                        modifier = Modifier
                                            .wrapContentWidth(Alignment.Start)  // Allow text to overflow
                                            .wrapContentHeight(Alignment.Top)
                                            .clickable { onTextClick(index, textField) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
*/


//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.gestures.detectTransformGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.nativeCanvas
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.tocho.mb3.MarkingItem
//
//@Composable
//fun GridComponent(
//    rows: Int = 20,
//    columns: Int = 80,
//    textFields: MutableMap<Int, MarkingItem>,
//    onCellClick: (Int) -> Unit,
//    onTextClick: (Int, MarkingItem) -> Unit,
//    sendTestDataToMB3: () -> Unit,
//    sendTextFieldsToMB3: (MutableMap<Int, MarkingItem>) -> Unit,
//    gridColor: Color = Color.LightGray,
//    lineThickness: Float = 1f
//) {
//    var scale by remember { mutableStateOf(1f) }
//    var offsetX by remember { mutableStateOf(0f) }
//    var offsetY by remember { mutableStateOf(0f) }
//    var isTextAddingEnabled by remember { mutableStateOf(true) }
//
//    val panZoomModifier = Modifier.pointerInput(Unit) {
//        detectTransformGestures { _, pan, zoom, _ ->
//            scale *= zoom
//            offsetX += pan.x
//            offsetY += pan.y
//        }
//    }
//
//    Column(modifier = Modifier.fillMaxSize().padding(0.dp)) {
//        Row(
//            verticalAlignment = Alignment.Top,
//            horizontalArrangement = Arrangement.Start
//        ) {
//            Button(onClick = { isTextAddingEnabled = !isTextAddingEnabled }) {
//                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
//            }
//
//            Button(onClick = { sendTestDataToMB3() }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("Send")
//            }
//
//            Button(onClick = { sendTextFieldsToMB3(textFields) }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("SendT")
//            }
//
//            Button(onClick = { scale += 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom In") }
//            Button(onClick = { scale -= 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom Out") }
//        }
//
//        Row(modifier = Modifier.fillMaxSize()) {
//            Column(
//                modifier = Modifier.padding(top = 4.dp, start = 0.dp),
//                verticalArrangement = Arrangement.spacedBy(4.dp)
//            ) {
//                Button(onClick = { /* Action A */ }) { Text("B") }
//                Button(onClick = { /* Action B */ }) { Text("C") }
//                Button(onClick = { /* Action C */ }) { Text("D") }
//                Button(onClick = { /* Action D */ }) { Text("E") }
//            }
//
//            BoxWithConstraints(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(16.dp)
//                    .then(panZoomModifier)
//            ) {
//                val density = LocalDensity.current
//                val cellWidthPx = with(density) { (maxWidth / columns).toPx() * scale }
//                val cellHeightPx = with(density) { (maxHeight / rows).toPx() * scale }
//
//                Canvas(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .offset(x = offsetX.dp, y = offsetY.dp)
//                ) {
//                    val gridWidth = cellWidthPx * columns
//                    val gridHeight = cellHeightPx * rows
//
//                    val adjustedOffsetX = (offsetX % cellWidthPx).coerceAtLeast(0f)
//                    val adjustedOffsetY = (offsetY % cellHeightPx).coerceAtLeast(0f)
//
//                    for (i in 0..columns) {
//                        val startX = i * cellWidthPx + adjustedOffsetX
//                        drawContext.canvas.nativeCanvas.drawText(
//                            "$i",
//                            startX + cellWidthPx / 2,
//                            adjustedOffsetY - 20f,
//                            android.graphics.Paint().apply {
//                                textSize = 20f * scale
//                                textAlign = android.graphics.Paint.Align.CENTER
//                            }
//                        )
//                    }
//
//                    for (i in 0..rows) {
//                        val startY = i * cellHeightPx + adjustedOffsetY
//                        drawContext.canvas.nativeCanvas.drawText(
//                            "$i",
//                            adjustedOffsetX - 20f,
//                            startY + cellHeightPx / 2,
//                            android.graphics.Paint().apply {
//                                textSize = 20f * scale
//                                textAlign = android.graphics.Paint.Align.RIGHT
//                            }
//                        )
//                    }
//
//                    for (i in 0..columns) {
//                        val startX = i * cellWidthPx + adjustedOffsetX
//                        drawLine(
//                            color = gridColor,
//                            start = Offset(startX, 0f),
//                            end = Offset(startX, gridHeight),
//                            strokeWidth = lineThickness
//                        )
//                    }
//
//                    for (i in 0..rows) {
//                        val startY = i * cellHeightPx + adjustedOffsetY
//                        drawLine(
//                            color = gridColor,
//                            start = Offset(0f, startY),
//                            end = Offset(gridWidth, startY),
//                            strokeWidth = lineThickness
//                        )
//                    }
//                }
//
//                LazyVerticalGrid(
//                    columns = GridCells.Fixed(columns),
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .offset(x = offsetX.dp, y = offsetY.dp)
//                ) {
//                    items(rows * columns) { index ->
//                        val col = index % columns
//                        val row = index / columns
//
//                        Box(
//                            modifier = Modifier
//                                .offset(
//                                    x = with(density) { (col * cellWidthPx).toDp() },
//                                    y = with(density) { (row * cellHeightPx).toDp() }
//                                )
//                                .clickable {
//                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
//                                        onCellClick(index)
//                                    }
//                                },
//                            contentAlignment = Alignment.TopStart
//                        ) {
//                            textFields[index]?.let { textField ->
//                                if (textField.text.isNotBlank()) {
//                                    // Calculate font size in terms of rows
//                                    val fontSizeInRows = textField.height  // This is the number of rows the text should span
//                                    val fontSize = (fontSizeInRows * cellHeightPx / density.density).sp  // Convert to sp
//
//                                    val scaleX = textField.widthPercentage / 100 * scale  // Scale width
//
//                                    Text(
//                                        text = textField.text,
//                                        fontSize = fontSize,  // Use calculated font size
//                                        textAlign = TextAlign.Start,
//                                        modifier = Modifier
//                                            .wrapContentWidth(Alignment.Start)
//                                            .wrapContentHeight(Alignment.Top)
//                                            .scale(scaleX = scaleX, scaleY = scale)
//                                            .clickable { onTextClick(index, textField) }
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

/*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tocho.mb3.MarkingItem

@Composable
fun GridComponent(
    textFields: MutableMap<Int, MarkingItem>,
    onCellClick: (Int) -> Unit,
    onTextClick: (Int, MarkingItem) -> Unit,
    sendTestDataToMB3: () -> Unit,
    sendTextFieldsToMB3: (MutableMap<Int, MarkingItem>) -> Unit,
    gridColor: Color = Color.LightGray,
    lineThickness: Float = 1f
) {
    // Predefined grid sizes
    val gridSizes = listOf(
        "80 x 20" to (80 to 20),
        "33 x 15" to (33 to 15),
        "20 x 15" to (20 to 15),
        "15 x 15" to (15 to 15),
        "10 x 10" to (10 to 10)
    )

    // State for managing the current selected grid size
    var selectedGridSize by remember { mutableStateOf(gridSizes[0].first) }
    var expanded by remember { mutableStateOf(false) }
    val (columns, rows) = gridSizes.first { it.first == selectedGridSize }.second

    // State for zoom and pan
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // State to track whether text-adding is enabled
    var isTextAddingEnabled by remember { mutableStateOf(true) }

    // Gesture detector for panning and zooming
    val panZoomModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            scale *= zoom
            offsetX += pan.x
            offsetY += pan.y
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(0.dp)) {
        // Top row with buttons and dropdown
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            Button(onClick = { isTextAddingEnabled = !isTextAddingEnabled }) {
                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
            }

            Button(onClick = { sendTestDataToMB3() }, modifier = Modifier.padding(start = 4.dp)) {
                Text("Send")
            }

            Button(onClick = { sendTextFieldsToMB3(textFields) }, modifier = Modifier.padding(start = 4.dp)) {
                Text("SendT")
            }

            // Button for selecting grid size with DropdownMenu
            Box {
                Button(onClick = { expanded = true }) {
                    Text("Grid Size: $selectedGridSize")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    gridSizes.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size.first) },
                            onClick = {
                                selectedGridSize = size.first
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(onClick = { scale += 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom In") }
            Button(onClick = { scale -= 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom Out") }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(top = 4.dp, start = 0.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(onClick = { /* Action A */ }) { Text("B") }
                Button(onClick = { /* Action B */ }) { Text("C") }
                Button(onClick = { /* Action C */ }) { Text("D") }
                Button(onClick = { /* Action D */ }) { Text("E") }
            }

            // Grid Layout
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .then(panZoomModifier)  // Add pan and zoom modifier here
            ) {
                val density = LocalDensity.current  // Use LocalDensity to convert Dp to Px
                // Calculate the smallest dimension to keep the grid cells square
                val cellSizePx = with(density) {
                    minOf(maxWidth / columns, maxHeight / rows).toPx() * scale
                }
                val cellWidthPx = with(density) { (maxWidth / columns).toPx() * scale }
                val cellHeightPx = with(density) { (maxHeight / rows).toPx() * scale }

                // Canvas for drawing numbers and grid together
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY.dp)  // Apply the same offset as the grid
                ) {
                    val gridWidth = cellWidthPx * columns
                    val gridHeight = cellHeightPx * rows

                    // Adjust starting positions for top and left numbers based on the current panning
                    val adjustedOffsetX = (offsetX % cellWidthPx).coerceAtLeast(0f)
                    val adjustedOffsetY = (offsetY % cellHeightPx).coerceAtLeast(0f)

                    // Draw vertical numbers above the grid (top row numbers)
                    for (i in 0..columns) {
                        val startX = i * cellWidthPx + adjustedOffsetX
                        drawContext.canvas.nativeCanvas.drawText(
                            "$i",
                            startX + cellWidthPx / 2,
                            adjustedOffsetY - 20f,  // Align numbers above grid lines
                            android.graphics.Paint().apply {
                                textSize = 20f * scale  // Scale the text size with zoom
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }

                    // Draw horizontal numbers to the left of the grid (left column numbers)
                    for (i in 0..rows) {
                        val startY = i * cellHeightPx + adjustedOffsetY
                        drawContext.canvas.nativeCanvas.drawText(
                            "$i",
                            adjustedOffsetX - 20f,  // Align numbers to the left of grid lines
                            startY + cellHeightPx / 2,
                            android.graphics.Paint().apply {
                                textSize = 20f * scale  // Scale the text size with zoom
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )
                    }

                    // Draw vertical grid lines
                    for (i in 0..columns) {
                        val startX = i * cellWidthPx + adjustedOffsetX
                        drawLine(
                            color = gridColor,
                            start = Offset(startX, 0f),
                            end = Offset(startX, gridHeight),
                            strokeWidth = lineThickness
                        )
                    }

                    // Draw horizontal grid lines
                    for (i in 0..rows) {
                        val startY = i * cellHeightPx + adjustedOffsetY
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, startY),
                            end = Offset(gridWidth, startY),
                            strokeWidth = lineThickness
                        )
                    }
                }

                // Text fields overlay and click handlers
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY.dp)
                ) {
                    for (index in 0 until rows * columns) {
                        val col = index % columns
                        val row = index / columns

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = with(density) { (col * cellWidthPx).toDp() },
                                    y = with(density) { ((row + 1) * cellHeightPx - (textFields[index]?.height ?: 1) * cellHeightPx).toDp() }
                                )
                                .clickable {
                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
                                        onCellClick(index)
                                    }
                                },
                            contentAlignment = Alignment.TopStart
                        ) {
                            textFields[index]?.let { textField ->
                                if (textField.text.isNotBlank()) {
                                    // Calculate the font size based on the grid's cell height and the number of rows the text should cover
                                    val fontSize = (textField.height * cellHeightPx / density.density).sp

                                    // Ensure the text is left-aligned and flows left to right
                                    Text(
                                        text = textField.text,
                                        fontSize = fontSize,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier
                                            .wrapContentWidth(Alignment.Start)
                                            .wrapContentHeight(Alignment.Top)
                                            .clickable { onTextClick(index, textField) }
                                    )
                                }
                            }
//                            textFields[index]?.let { textField ->
//                                if (textField.text.isNotBlank()) {
//                                    // Calculate the font size based on the height defined in MarkingItem
//                                    val fontSize = (textField.height * scale).sp  // Use height from MarkingItem
//
//                                    // Ensure the text is left-aligned and flows left to right
//                                    Text(
//                                        text = textField.text,
//                                        fontSize = fontSize,
//                                        textAlign = TextAlign.Start,
//                                        modifier = Modifier
//                                            .wrapContentWidth(Alignment.Start)
//                                            .wrapContentHeight(Alignment.Top)
//                                            .clickable { onTextClick(index, textField) }
//                                    )
//                                }
//                            }
//                            textFields[index]?.let { textField ->
//                                if (textField.text.isNotBlank()) {
//                                    val fontSize = (cellHeightPx / density.density).sp
//
//                                    // Ensure the text is left-aligned and flows left to right
//                                    Text(
//                                        text = textField.text,
//                                        fontSize = fontSize,
//                                        textAlign = TextAlign.Start,
//                                        modifier = Modifier
//                                            .wrapContentWidth(Alignment.Start)
//                                            .wrapContentHeight(Alignment.Top)
//                                            .clickable { onTextClick(index, textField) }
//                                    )
//                                }
//                            }
                        }
                    }
                }
            }
        }
    }
}
*/


//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.gestures.detectTransformGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.nativeCanvas
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.tocho.mb3.MarkingItem
//import kotlin.math.roundToInt
//
////TODO: tapping multiple parts of the grid creates multiple textfields. Need to make it only create one at a time
//@Composable
//fun GridComponent(
//    rows: Int = 20,
//    columns: Int = 80,
//    textFields: MutableMap<Int, MarkingItem>,
//    onCellClick: (Int) -> Unit,
//    onTextClick: (Int, MarkingItem) -> Unit,
//    sendTestDataToMB3: () -> Unit,
//    sendTextFieldsToMB3: (MutableMap<Int, MarkingItem>) -> Unit,  // New function to send text fields data
////    sendTextFieldsToMB3: () -> Unit,  // Add this parameter
//    gridColor: Color = Color.LightGray,
//    lineThickness: Float = 1f
//) {
//    // State for zoom and pan
//    var scale by remember { mutableStateOf(1f) }
//    var offsetX by remember { mutableStateOf(0f) }
//    var offsetY by remember { mutableStateOf(0f) }
//
//    // State to track the current grid size
//    var gridSizeIndex by remember { mutableStateOf(0) }
//
//    // Predefined grid sizes
//    val gridSizes = listOf(
//        80 to 20,  // 80x20
//        33 to 15,  // 33x15
//        20 to 15,  // 20x15
//        15 to 15,  // 15x15
//        10 to 10   // 10x10
//    )
//
//    // Get the current rows and columns based on the index
//    val (columns, rows) = gridSizes[gridSizeIndex]
//
//    // State to track whether text-adding is enabled
//    var isTextAddingEnabled by remember { mutableStateOf(true) }
//
//    // Gesture detector for panning and zooming
//    val panZoomModifier = Modifier.pointerInput(Unit) {
//        detectTransformGestures { _, pan, zoom, _ ->
//            scale *= zoom
//            offsetX += pan.x
//            offsetY += pan.y
//        }
//    }
//
//    Column(modifier = Modifier.fillMaxSize().padding(0.dp)) {
//        // Top row with buttons
//        Row(
//            verticalAlignment = Alignment.Top,
//            horizontalArrangement = Arrangement.Start
//        ) {
//            Button(onClick = { isTextAddingEnabled = !isTextAddingEnabled }) {
//                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
//            }
//
//            Button(onClick = { sendTestDataToMB3() }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("Send")
//            }
//
//            Button(onClick = { sendTextFieldsToMB3(textFields) }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("SendT")
//            }
//
//            // Button to change grid dimensions
//            Button(onClick = {
//                gridSizeIndex = (gridSizeIndex + 1) % gridSizes.size
//            }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("Change Grid Size")
//            }
//
//            Button(onClick = { scale += 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom In") }
//            Button(onClick = { scale -= 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom Out") }
//        }
//
//        Row(modifier = Modifier.fillMaxSize()) {
//            Column(
//                modifier = Modifier.padding(top = 4.dp, start = 0.dp),
//                verticalArrangement = Arrangement.spacedBy(4.dp)
//            ) {
//                Button(onClick = { /* Action A */ }) { Text("B") }
//                Button(onClick = { /* Action B */ }) { Text("C") }
//                Button(onClick = { /* Action C */ }) { Text("D") }
//                Button(onClick = { /* Action D */ }) { Text("E") }
//            }
//
//            // Grid Layout
//            BoxWithConstraints(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(16.dp)
//                    .then(panZoomModifier)  // Add pan and zoom modifier here
//            ) {
//                val density = LocalDensity.current  // Use LocalDensity to convert Dp to Px
//                val cellWidthPx = with(density) { (maxWidth / columns).toPx() * scale }
//                val cellHeightPx = with(density) { (maxHeight / rows).toPx() * scale }
//
//                // Canvas for drawing numbers and grid together
//                Canvas(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .offset(x = offsetX.dp, y = offsetY.dp)  // Apply the same offset as the grid
//                ) {
//                    val gridWidth = cellWidthPx * columns
//                    val gridHeight = cellHeightPx * rows
//
//                    // Adjust starting positions for top and left numbers based on the current panning
//                    val adjustedOffsetX = (offsetX % cellWidthPx).coerceAtLeast(0f)
//                    val adjustedOffsetY = (offsetY % cellHeightPx).coerceAtLeast(0f)
//
//                    // Draw vertical numbers above the grid (top row numbers)
//                    for (i in 0..columns) {
//                        val startX = i * cellWidthPx + adjustedOffsetX
//                        drawContext.canvas.nativeCanvas.drawText(
//                            "$i",
//                            startX + cellWidthPx / 2,
//                            adjustedOffsetY - 20f,  // Align numbers above grid lines
//                            android.graphics.Paint().apply {
//                                textSize = 20f * scale  // Scale the text size with zoom
//                                textAlign = android.graphics.Paint.Align.CENTER
//                            }
//                        )
//                    }
//
//                    // Draw horizontal numbers to the left of the grid (left column numbers)
//                    for (i in 0..rows) {
//                        val startY = i * cellHeightPx + adjustedOffsetY
//                        drawContext.canvas.nativeCanvas.drawText(
//                            "$i",
//                            adjustedOffsetX - 20f,  // Align numbers to the left of grid lines
//                            startY + cellHeightPx / 2,
//                            android.graphics.Paint().apply {
//                                textSize = 20f * scale  // Scale the text size with zoom
//                                textAlign = android.graphics.Paint.Align.RIGHT
//                            }
//                        )
//                    }
//
//                    // Draw vertical grid lines
//                    for (i in 0..columns) {
//                        val startX = i * cellWidthPx + adjustedOffsetX
//                        drawLine(
//                            color = gridColor,
//                            start = Offset(startX, 0f),
//                            end = Offset(startX, gridHeight),
//                            strokeWidth = lineThickness
//                        )
//                    }
//
//                    // Draw horizontal grid lines
//                    for (i in 0..rows) {
//                        val startY = i * cellHeightPx + adjustedOffsetY
//                        drawLine(
//                            color = gridColor,
//                            start = Offset(0f, startY),
//                            end = Offset(gridWidth, startY),
//                            strokeWidth = lineThickness
//                        )
//                    }
//                }
//
//                // Text fields overlay and click handlers
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .offset(x = offsetX.dp, y = offsetY.dp)
//                ) {
//                    for (index in 0 until rows * columns) {
//                        val col = index % columns
//                        val row = index / columns
//
//                        Box(
//                            modifier = Modifier
//                                .offset(
//                                    x = with(density) { (col * cellWidthPx).toDp() },
//                                    y = with(density) { (row * cellHeightPx).toDp() }
//                                )
//                                .clickable {
//                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
//                                        onCellClick(index)
//                                    }
//                                },
//                            contentAlignment = Alignment.TopStart
//                        ) {
//                            textFields[index]?.let { textField ->
//                                if (textField.text.isNotBlank()) {
//                                    val fontSize = textField.height.sp
//                                    val scaleX = textField.widthPercentage / 100
//                                    Text(
//                                        text = textField.text,
//                                        fontSize = fontSize,
//                                        textAlign = TextAlign.Start,
//                                        maxLines = 1,
//                                        modifier = Modifier
//                                            .wrapContentWidth(Alignment.Start)
//                                            .wrapContentHeight(Alignment.Top)
//                                            .scale(scaleX = scaleX, scaleY = 1f)
//                                            .clickable { onTextClick(index, textField) }
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}


/*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tocho.mb3.MarkingItem

@Composable
fun GridComponent(
    rows: Int = 20,
    columns: Int = 80,
    textFields: MutableMap<Int, MarkingItem>,
    onCellClick: (Int) -> Unit,
    onTextClick: (Int, MarkingItem) -> Unit,
    sendTestDataToMB3: () -> Unit,
    gridColor: Color = Color.LightGray,
    lineThickness: Float = 1f
) {
    // State for zoom and pan
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Track whether a tap is already being processed
    var isProcessingTap by remember { mutableStateOf(false) }

    // State to track whether text-adding is enabled
    var isTextAddingEnabled by remember { mutableStateOf(true) }

    val panZoomModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            // Handle panning and zooming with gestures
            offsetX += pan.x
            offsetY += pan.y
            scale *= zoom
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(0.dp)) {
        // Top row with buttons
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            Button(onClick = { isTextAddingEnabled = !isTextAddingEnabled }) {
                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
            }

            Button(onClick = { sendTestDataToMB3() }, modifier = Modifier.padding(start = 4.dp)) {
                Text("Send")
            }

            Button(onClick = { scale += 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom In") }
            Button(onClick = { scale -= 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom Out") }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(top = 4.dp, start = 0.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(onClick = { /* Action A */ }) { Text("B") }
                Button(onClick = { /* Action B */ }) { Text("C") }
                Button(onClick = { /* Action C */ }) { Text("D") }
                Button(onClick = { /* Action D */ }) { Text("E") }
            }

            // Grid Layout
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .then(panZoomModifier)  // Add pan and zoom modifier here
            ) {
                val cellWidth = (maxWidth / columns) * scale
                val cellHeight = (maxHeight / rows) * scale

                // Canvas for drawing the grid and numbers
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    // Ensure only one tap is processed at a time
                                    if (!isProcessingTap) {
                                        isProcessingTap = true

                                        // Calculate which cell was tapped
                                        val col = ((tapOffset.x - offsetX) / cellWidth.toPx()).toInt()
                                        val row = ((tapOffset.y - offsetY) / cellHeight.toPx()).toInt()

                                        if (col in 0 until columns && row in 0 until rows) {
                                            val index = row * columns + col
                                            if (isTextAddingEnabled && !textFields.containsKey(index)) {
                                                onCellClick(index)
                                            }
                                        }

                                        // Reset the flag after processing the tap
                                        isProcessingTap = false
                                    }
                                }
                            )
                        }
                ) {
                    val gridWidth = cellWidth.toPx() * columns
                    val gridHeight = cellHeight.toPx() * rows

                    // Draw grid lines
                    for (i in 0..columns) {
                        val startX = i * cellWidth.toPx()
                        drawLine(
                            color = gridColor,
                            start = Offset(startX, 0f),
                            end = Offset(startX, gridHeight),
                            strokeWidth = lineThickness
                        )
                    }

                    for (i in 0..rows) {
                        val startY = i * cellHeight.toPx()
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, startY),
                            end = Offset(gridWidth, startY),
                            strokeWidth = lineThickness
                        )
                    }
                }

                // Text fields overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY.dp)
                ) {
                    for (index in 0 until rows * columns) {
                        val col = index % columns
                        val row = index / columns

                        Box(
                            modifier = Modifier
                                .offset(x = (col * cellWidth).toPx().dp, y = (row * cellHeight).toPx().dp)
                                .clickable {
                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
                                        onCellClick(index)
                                    }
                                },
                            contentAlignment = Alignment.TopStart
                        ) {
                            textFields[index]?.let { textField ->
                                if (textField.text.isNotBlank()) {
                                    val fontSize = textField.height.sp
                                    Text(
                                        text = textField.text,
                                        fontSize = fontSize,
                                        textAlign = TextAlign.Start,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .wrapContentWidth(Alignment.Start)
                                            .wrapContentHeight(Alignment.Top)
                                            .clickable { onTextClick(index, textField) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
*/

//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.gestures.awaitEachGesture
//import androidx.compose.foundation.gestures.awaitFirstDown
//import androidx.compose.foundation.gestures.calculatePan
//import androidx.compose.foundation.gestures.calculateZoom
//import androidx.compose.foundation.gestures.detectTransformGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.nativeCanvas
//import androidx.compose.ui.input.pointer.changedToUp
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.tocho.mb3.MarkingItem
//
//@Composable
//fun GridComponent(
//    rows: Int = 20,
//    columns: Int = 80,
//    textFields: MutableMap<Int, MarkingItem>,
//    onCellClick: (Int) -> Unit,
//    onTextClick: (Int, MarkingItem) -> Unit,
//    sendTestDataToMB3: () -> Unit,
//    gridColor: Color = Color.LightGray,
//    lineThickness: Float = 1f
//) {
//    // State for zoom and pan
//    var scale by remember { mutableStateOf(1f) }
//    var offsetX by remember { mutableStateOf(0f) }
//    var offsetY by remember { mutableStateOf(0f) }
//
//    // State to track whether text-adding is enabled
//    var isTextAddingEnabled by remember { mutableStateOf(true) }
//
//    // Gesture detector for pan and zoom
//    val panZoomModifier = Modifier.pointerInput(Unit) {
//        val density = LocalDensity.current
//        // Calculate the width and height of a single grid cell in pixels
//        val cellWidthPx = with(density) { (maxWidth / columns).toPx() * scale }
//        val cellHeightPx = with(density) { (maxHeight / rows).toPx() * scale }
//
//        awaitPointerEventScope {
//            while (true) {
//                // Wait for the next pointer event
//                val event = awaitPointerEvent()
//
//                // Count the number of pointers (fingers) on the screen
//                val pointerCount = event.changes.size
//
//                // If only one finger is used, treat it as a tap
//                if (pointerCount == 1) {
//                    val pointer = event.changes[0]
//
//                    // Check if it was a tap (no movement, short duration)
//                    if (pointer.changedToUp()) {
//                        val tapPosition = pointer.position
//                        val col = ((tapPosition.x - offsetX) / cellWidthPx).toInt()
//                        val row = ((tapPosition.y - offsetY) / cellHeightPx).toInt()
//
//                        // Ensure it's a valid grid cell
//                        if (col in 0 until columns && row in 0 until rows) {
//                            onCellClick(row * columns + col)
//                        }
//                    }
//                }
//
//                // Handle panning only when using 2 fingers
//                if (pointerCount == 2) {
//                    val pan = event.calculatePan()
//                    offsetX += pan.x
//                    offsetY += pan.y
//                }
//
//                // Handle zoom regardless of the number of fingers
//                val zoomChange = event.calculateZoom()
//                scale *= zoomChange
//
//                // Break out of loop when all fingers are lifted up
//                if (event.changes.all { it.changedToUp() }) {
//                    break
//                }
//            }
//        }
//    }
//
//
//    Column(modifier = Modifier.fillMaxSize().padding(0.dp)) {
//        // Top row with buttons
//        Row(
//            verticalAlignment = Alignment.Top,
//            horizontalArrangement = Arrangement.Start
//        ) {
//            Button(onClick = { isTextAddingEnabled = !isTextAddingEnabled }) {
//                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
//            }
//
//            Button(onClick = { sendTestDataToMB3() }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("Send")
//            }
//
//            Button(onClick = { scale += 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom In") }
//            Button(onClick = { scale -= 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom Out") }
//        }
//
//        Row(modifier = Modifier.fillMaxSize()) {
//            Column(
//                modifier = Modifier.padding(top = 4.dp, start = 0.dp),
//                verticalArrangement = Arrangement.spacedBy(4.dp)
//            ) {
//                Button(onClick = { /* Action A */ }) { Text("B") }
//                Button(onClick = { /* Action B */ }) { Text("C") }
//                Button(onClick = { /* Action C */ }) { Text("D") }
//                Button(onClick = { /* Action D */ }) { Text("E") }
//            }
//
//            // Grid Layout
//            BoxWithConstraints(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(16.dp)
//                    .then(panZoomModifier)  // Apply pan and zoom modifier here
//            ) {
//                val density = LocalDensity.current  // Use LocalDensity to convert Dp to Px
//                val cellWidthPx = with(density) { (maxWidth / columns).toPx() * scale }
//                val cellHeightPx = with(density) { (maxHeight / rows).toPx() * scale }
//
//                // Canvas for drawing numbers and grid together
//                Canvas(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .offset(x = offsetX.dp, y = offsetY.dp)  // Apply panning offset
//                ) {
//                    val gridWidth = cellWidthPx * columns
//                    val gridHeight = cellHeightPx * rows
//
//                    // Adjust starting positions for top and left numbers based on current panning
//                    val adjustedOffsetX = (offsetX % cellWidthPx).coerceAtLeast(0f)
//                    val adjustedOffsetY = (offsetY % cellHeightPx).coerceAtLeast(0f)
//
//                    // Draw vertical numbers above the grid (top row numbers)
//                    for (i in 0..columns) {
//                        val startX = i * cellWidthPx + adjustedOffsetX
//                        drawContext.canvas.nativeCanvas.drawText(
//                            "$i",
//                            startX + cellWidthPx / 2,
//                            adjustedOffsetY - 20f,  // Align numbers above grid lines
//                            android.graphics.Paint().apply {
//                                textSize = 20f * scale  // Scale text size with zoom
//                                textAlign = android.graphics.Paint.Align.CENTER
//                            }
//                        )
//                    }
//
//                    // Draw horizontal numbers to the left of the grid (left column numbers)
//                    for (i in 0..rows) {
//                        val startY = i * cellHeightPx + adjustedOffsetY
//                        drawContext.canvas.nativeCanvas.drawText(
//                            "$i",
//                            adjustedOffsetX - 20f,  // Align numbers to the left of grid lines
//                            startY + cellHeightPx / 2,
//                            android.graphics.Paint().apply {
//                                textSize = 20f * scale  // Scale text size with zoom
//                                textAlign = android.graphics.Paint.Align.RIGHT
//                            }
//                        )
//                    }
//
//                    // Draw vertical grid lines
//                    for (i in 0..columns) {
//                        val startX = i * cellWidthPx + adjustedOffsetX
//                        drawLine(
//                            color = gridColor,
//                            start = Offset(startX, 0f),
//                            end = Offset(startX, gridHeight),
//                            strokeWidth = lineThickness
//                        )
//                    }
//
//                    // Draw horizontal grid lines
//                    for (i in 0..rows) {
//                        val startY = i * cellHeightPx + adjustedOffsetY
//                        drawLine(
//                            color = gridColor,
//                            start = Offset(0f, startY),
//                            end = Offset(gridWidth, startY),
//                            strokeWidth = lineThickness
//                        )
//                    }
//                }
//
//                // Text fields overlay and click handlers
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .offset(x = offsetX.dp, y = offsetY.dp)
//                ) {
//                    for (index in 0 until rows * columns) {
//                        val col = index % columns
//                        val row = index / columns
//
//                        Box(
//                            modifier = Modifier
//                                .offset(
//                                    x = with(density) { (col * cellWidthPx).toDp() },
//                                    y = with(density) { (row * cellHeightPx).toDp() }
//                                )
//                                .clickable {
//                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
//                                        onCellClick(index)
//                                    }
//                                },
//                            contentAlignment = Alignment.TopStart
//                        ) {
//                            textFields[index]?.let { textField ->
//                                if (textField.text.isNotBlank()) {
//                                    val fontSize = textField.height.sp
//                                    val scaleX = textField.widthPercentage / 100
//                                    Text(
//                                        text = textField.text,
//                                        fontSize = fontSize,
//                                        textAlign = TextAlign.Start,
//                                        maxLines = 1,
//                                        modifier = Modifier
//                                            .wrapContentWidth(Alignment.Start)
//                                            .wrapContentHeight(Alignment.Top)
//                                            .scale(scaleX = scaleX, scaleY = 1f)
//                                            .clickable { onTextClick(index, textField) }
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}


///here?
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.gestures.calculatePan
//import androidx.compose.foundation.gestures.calculateZoom
//import androidx.compose.foundation.gestures.detectTransformGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.nativeCanvas
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.layout.layoutId
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.unit.times
//import com.tocho.mb3.MarkingItem
//
//@Composable
//fun GridComponent(
//    rows: Int = 20,
//    columns: Int = 80,
//    textFields: MutableMap<Int, MarkingItem>,
//    onCellClick: (Int) -> Unit,
//    onTextClick: (Int, MarkingItem) -> Unit,
//    sendTestDataToMB3: () -> Unit,
//    gridColor: Color = Color.LightGray,
//    lineThickness: Float = 1f
//) {
//    // State for zoom and pan
//    var scale by remember { mutableStateOf(1f) }
//    var offsetX by remember { mutableStateOf(0f) }
//    var offsetY by remember { mutableStateOf(0f) }
//
//    // State to track whether text-adding is enabled
//    var isTextAddingEnabled by remember { mutableStateOf(true) }
//
//    val panZoomModifier = Modifier.pointerInput(Unit) {
//        awaitPointerEventScope {
//            while (true) {
//                // Get the next pointer event
//                val event = awaitPointerEvent()
//
//                // Get the number of pointers (fingers) currently on the screen
//                val pointerCount = event.changes.size
//
//                // Only allow panning when using 2 fingers
//                if (pointerCount == 2) {
//                    val pan = event.calculatePan()
//                    offsetX += pan.x
//                    offsetY += pan.y
//                }
//
//                // Handle zoom regardless of the number of fingers
//                val zoomChange = event.calculateZoom()
//                scale *= zoomChange
//            }
//        }
//    }
//
//    Column(modifier = Modifier.fillMaxSize().padding(0.dp)) {
//        // Top row with buttons
//        Row(
//            verticalAlignment = Alignment.Top,
//            horizontalArrangement = Arrangement.Start
//        ) {
//            Button(onClick = { isTextAddingEnabled = !isTextAddingEnabled }) {
//                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
//            }
//
//            Button(onClick = { sendTestDataToMB3() }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("Send")
//            }
//
//            Button(onClick = { scale += 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom In") }
//            Button(onClick = { scale -= 0.1f }, modifier = Modifier.padding(start = 4.dp)) { Text("Zoom Out") }
//        }
//
//        Row(modifier = Modifier.fillMaxSize()) {
//            Column(
//                modifier = Modifier.padding(top = 4.dp, start = 0.dp),
//                verticalArrangement = Arrangement.spacedBy(4.dp)
//            ) {
//                Button(onClick = { /* Action A */ }) { Text("B") }
//                Button(onClick = { /* Action B */ }) { Text("C") }
//                Button(onClick = { /* Action C */ }) { Text("D") }
//                Button(onClick = { /* Action D */ }) { Text("E") }
//            }
//
//            // Grid Layout
//            BoxWithConstraints(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(16.dp)
//                    .then(panZoomModifier)  // Add pan and zoom modifier here
//            ) {
//                val density = LocalDensity.current  // Use LocalDensity to convert Dp to Px
//                val cellWidthPx = with(density) { (maxWidth / columns).toPx() * scale }
//                val cellHeightPx = with(density) { (maxHeight / rows).toPx() * scale }
//
//                // Canvas for drawing numbers and grid together
//                Canvas(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .offset(x = offsetX.dp, y = offsetY.dp)  // Apply the same offset as the grid
//                ) {
//                    val gridWidth = cellWidthPx * columns
//                    val gridHeight = cellHeightPx * rows
//
//                    // Adjust starting positions for top and left numbers based on the current panning
//                    val adjustedOffsetX = (offsetX % cellWidthPx).coerceAtLeast(0f)
//                    val adjustedOffsetY = (offsetY % cellHeightPx).coerceAtLeast(0f)
//
//                    // Draw vertical numbers above the grid (top row numbers)
//                    for (i in 0..columns) {
//                        val startX = i * cellWidthPx + adjustedOffsetX
//                        drawContext.canvas.nativeCanvas.drawText(
//                            "$i",
//                            startX + cellWidthPx / 2,
//                            adjustedOffsetY - 20f,  // Align numbers above grid lines
//                            android.graphics.Paint().apply {
//                                textSize = 20f * scale  // Scale the text size with zoom
//                                textAlign = android.graphics.Paint.Align.CENTER
//                            }
//                        )
//                    }
//
//                    // Draw horizontal numbers to the left of the grid (left column numbers)
//                    for (i in 0..rows) {
//                        val startY = i * cellHeightPx + adjustedOffsetY
//                        drawContext.canvas.nativeCanvas.drawText(
//                            "$i",
//                            adjustedOffsetX - 20f,  // Align numbers to the left of grid lines
//                            startY + cellHeightPx / 2,
//                            android.graphics.Paint().apply {
//                                textSize = 20f * scale  // Scale the text size with zoom
//                                textAlign = android.graphics.Paint.Align.RIGHT
//                            }
//                        )
//                    }
//
//                    // Draw vertical grid lines
//                    for (i in 0..columns) {
//                        val startX = i * cellWidthPx + adjustedOffsetX
//                        drawLine(
//                            color = gridColor,
//                            start = Offset(startX, 0f),
//                            end = Offset(startX, gridHeight),
//                            strokeWidth = lineThickness
//                        )
//                    }
//
//                    // Draw horizontal grid lines
//                    for (i in 0..rows) {
//                        val startY = i * cellHeightPx + adjustedOffsetY
//                        drawLine(
//                            color = gridColor,
//                            start = Offset(0f, startY),
//                            end = Offset(gridWidth, startY),
//                            strokeWidth = lineThickness
//                        )
//                    }
//                }
//
//                // Text fields overlay and click handlers
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .offset(x = offsetX.dp, y = offsetY.dp)
//                ) {
//                    for (index in 0 until rows * columns) {
//                        val col = index % columns
//                        val row = index / columns
//
//                        Box(
//                            modifier = Modifier
//                                .offset(
//                                    x = with(density) { (col * cellWidthPx).toDp() },
//                                    y = with(density) { (row * cellHeightPx).toDp() }
//                                )
//                                .clickable {
//                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
//                                        onCellClick(index)
//                                    }
//                                },
//                            contentAlignment = Alignment.TopStart
//                        ) {
//                            textFields[index]?.let { textField ->
//                                if (textField.text.isNotBlank()) {
//                                    val fontSize = textField.height.sp
//                                    val scaleX = textField.widthPercentage / 100
//                                    Text(
//                                        text = textField.text,
//                                        fontSize = fontSize,
//                                        textAlign = TextAlign.Start,
//                                        maxLines = 1,
//                                        modifier = Modifier
//                                            .wrapContentWidth(Alignment.Start)
//                                            .wrapContentHeight(Alignment.Top)
//                                            .scale(scaleX = scaleX, scaleY = 1f)
//                                            .clickable { onTextClick(index, textField) }
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}



//package com.tocho.mb3
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.min
//import androidx.compose.ui.unit.sp
//
//@Composable
//fun GridComponent(
//    rows: Int = 20,          // Number of rows
//    columns: Int = 20,       // Number of columns (equal to rows to ensure a square grid)
//    textFields: MutableMap<Int, MarkingItem>,  // Pass text fields for content
//    onCellClick: (Int) -> Unit,               // Handle cell click event
//    onTextClick: (Int, MarkingItem) -> Unit,  // Handle text click event
//    sendTestDataToMB3: () -> Unit,            // Pass the function reference from AdminModeActivity
//    gridColor: Color = Color.LightGray,       // Customizable color for the grid
//    lineThickness: Float = 1f                 // Line thickness for grid lines
//) {
//    // State to track whether text-adding is enabled
//    var isTextAddingEnabled by remember { mutableStateOf(true) }
//
//    // State for controlling zoom level (scale)
//    var scale by remember { mutableStateOf(1f) }
//
//    // Main layout with rows and columns
//    Column(
//        modifier = Modifier.fillMaxSize().padding(0.dp)  // No padding to keep buttons close to the edges
//    ) {
//        // Top row with buttons for zoom and toggling text-adding
//        Row(
//            verticalAlignment = Alignment.Top,
//            horizontalArrangement = Arrangement.Start
//        ) {
//            // Button to toggle text-adding
//            Button(
//                onClick = {
//                    isTextAddingEnabled = !isTextAddingEnabled  // Toggle text-adding ability
//                },
//                modifier = Modifier.padding(0.dp)
//            ) {
//                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
//            }
//
//            // Send button to call sendTestDataToMB3 directly
//            Button(
//                onClick = {
//                    sendTestDataToMB3()
//                },
//                modifier = Modifier.padding(start = 4.dp)
//            ) {
//                Text("Send")
//            }
//
//            // Zoom In button
//            Button(onClick = { scale += 0.1f }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("Zoom In")
//            }
//
//            // Zoom Out button
//            Button(onClick = { if (scale > 0.2f) scale -= 0.1f }, modifier = Modifier.padding(start = 4.dp)) {
//                Text("Zoom Out")
//            }
//        }
//
//        // Layout containing left buttons and grid
//        Row(modifier = Modifier.fillMaxSize()) {
//            // Left buttons (optional)
//            Column(
//                modifier = Modifier.padding(top = 4.dp, start = 0.dp),
//                verticalArrangement = Arrangement.spacedBy(4.dp)
//            ) {
//                Button(onClick = { /* TODO: Action A */ }) { Text("B") }
//                Button(onClick = { /* TODO: Action B */ }) { Text("C") }
//                Button(onClick = { /* TODO: Action C */ }) { Text("D") }
//                Button(onClick = { /* TODO: Action D */ }) { Text("E") }
//            }
//
//            // Grid Layout with zoom functionality
//            BoxWithConstraints(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(16.dp)
//                    .scale(scale)  // Apply scaling
//            ) {
//                val minSize = min(maxWidth / columns, maxHeight / rows)  // Ensure square cells
//                val cellSize: Dp = minSize
//
//                // Numbers on the top outside the grid
//                Row(
//                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    for (i in 0..columns) {
//                        Text(
//                            text = "$i",
//                            fontSize = 10.sp,
//                            modifier = Modifier.padding(start = cellSize / 2)
//                        )
//                    }
//                }
//
//                // Numbers on the left outside the grid
//                Column(
//                    modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
//                    verticalArrangement = Arrangement.SpaceBetween
//                ) {
//                    for (i in 0..rows) {
//                        Text(
//                            text = "$i",
//                            fontSize = 10.sp,
//                            modifier = Modifier.padding(top = cellSize / 2)
//                        )
//                    }
//                }
//
//                // Outer box containing the grid
//                Box(modifier = Modifier.fillMaxSize()) {
//                    // Canvas for drawing the grid
//                    Canvas(modifier = Modifier.fillMaxSize()) {
//                        val cellSizePx = size.width / columns
//                        val gridSize = cellSizePx * columns
//
//                        // Draw vertical lines
//                        for (i in 0..columns) {
//                            val startX = i * cellSizePx
//                            drawLine(
//                                color = gridColor,
//                                start = Offset(startX, 0f),
//                                end = Offset(startX, gridSize),
//                                strokeWidth = lineThickness
//                            )
//                        }
//
//                        // Draw horizontal lines
//                        for (i in 0..rows) {
//                            val startY = i * cellSizePx
//                            drawLine(
//                                color = gridColor,
//                                start = Offset(0f, startY),
//                                end = Offset(gridSize, startY),
//                                strokeWidth = lineThickness
//                            )
//                        }
//                    }
//
//                    // Overlay text fields and handle clicks
//                    for (index in 0 until rows * columns) {
//                        val col = index % columns
//                        val row = index / rows
//
//                        Box(
//                            modifier = Modifier
//                                .offset(
//                                    x = with(LocalDensity.current) { (col * cellSize.toPx()).toDp() },
//                                    y = with(LocalDensity.current) { (row * cellSize.toPx()).toDp() }
//                                )
//                                .size(cellSize)
//                                .clickable {
//                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
//                                        onCellClick(index)
//                                    }
//                                },
//                            contentAlignment = Alignment.TopStart
//                        ) {
//                            textFields[index]?.let { textField ->
//                                Text(
//                                    text = textField.text,
//                                    fontSize = textField.height.sp,
//                                    modifier = Modifier.clickable { onTextClick(index, textField) }
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

//package com.tocho.mb3
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//@Composable
//fun GridComponent(
//    rows: Int = 20,          // Number of rows
//    columns: Int = 80,       // Number of columns
//    textFields: MutableMap<Int, MarkingItem>,  // Pass text fields for content
//    onCellClick: (Int) -> Unit,               // Handle cell click event
//    onTextClick: (Int, MarkingItem) -> Unit,  // Handle text click event
//    sendTestDataToMB3: () -> Unit,            // Pass the function reference from AdminModeActivity
//    gridColor: Color = Color.LightGray,       // Customizable color for the grid
//    lineThickness: Float = 1f                 // Line thickness for grid lines
//) {
//    // State to track whether text-adding is enabled
//    var isTextAddingEnabled by remember { mutableStateOf(true) }
//
//    // Main layout with rows and columns
//    Column(
//        modifier = Modifier.fillMaxSize().padding(0.dp)  // No padding to keep buttons close to the edges
//    ) {
//        // Top row with buttons and grid numbers
//        Row(
//            verticalAlignment = Alignment.Top, // Align top buttons to the top of the screen
//            horizontalArrangement = Arrangement.Start  // Align leftmost button to the start (left)
//        ) {
//            // Top-left button (A) to toggle text-adding
//            Button(
//                onClick = {
//                    isTextAddingEnabled = !isTextAddingEnabled  // Toggle text-adding ability
//                },
//                modifier = Modifier.padding(0.dp)
//            ) {
//                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
//            }
//
//            // Send button to call sendTestDataToMB3 directly
//            Button(
//                onClick = {
//                    sendTestDataToMB3()
//                          },  // Directly calling sendTestDataToMB3
//                modifier = Modifier.padding(start = 4.dp)
//            ) {
//                Text("Send")
//            }
//
//            Button(onClick = { /* TODO: Action + */ }, modifier = Modifier.padding(start = 4.dp)) { Text("+") }
//            Button(onClick = { /* TODO: Action - */ }, modifier = Modifier.padding(start = 4.dp)) { Text("-") }
//        }
//
//        // Layout containing left buttons and grid
//        Row(modifier = Modifier.fillMaxSize()) {
//            // Left buttons (A, B, C, D) closely positioned to the left
//            Column(
//                modifier = Modifier.padding(top = 4.dp, start = 0.dp),  // Minimal padding to keep buttons close to the grid
//                verticalArrangement = Arrangement.spacedBy(4.dp)  // Minimal space between buttons
//            ) {
//                Button(onClick = { /* TODO: Action A */ }) { Text("B") }
//                Button(onClick = { /* TODO: Action B */ }) { Text("C") }
//                Button(onClick = { /* TODO: Action C */ }) { Text("D") }
//                Button(onClick = { /* TODO: Action D */ }) { Text("E") }
//            }
//
//            // Grid Layout
//            BoxWithConstraints(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(16.dp)  // Adjust padding around the whole grid and labels as needed
//            ) {
//                val cellWidth: Dp = maxWidth / columns
//                val cellHeight: Dp = maxHeight / rows
//
//                // Numbers on the top outside the grid
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(bottom = 8.dp),  // Add space between numbers and grid
//                    horizontalArrangement = Arrangement.SpaceBetween  // Evenly distribute numbers along the horizontal axis
//                ) {
//                    for (i in 0..columns) {  // Label every 5th column
//                        Text(
//                            text = "$i",
//                            fontSize = 5.sp,
//                            modifier = Modifier.padding(start = cellWidth / 2)  // Align number with the grid line
//                        )
//                    }
//                }
//
//                // Numbers on the left outside the grid
//                Column(
//                    modifier = Modifier
//                        .fillMaxHeight()
//                        .padding(end = 8.dp),  // Add space between numbers and grid
//                    verticalArrangement = Arrangement.SpaceBetween  // Evenly distribute numbers along the vertical axis
//                ) {
//                    for (i in 0..rows) {
//                        Text(
//                            text = "$i",  // Each row represents 5 mm
//                            fontSize = 5.sp,
//                            modifier = Modifier.padding(top = cellHeight / 2)  // Align number with the grid line
//                        )
//                    }
//                }
//
//                // Outer box containing the grid
//                Box(modifier = Modifier.fillMaxSize()) {
//                    // Canvas for drawing the grid itself
//                    Canvas(modifier = Modifier.fillMaxSize()) {
//                        val cellWidthPx = size.width / columns
//                        val cellHeightPx = size.height / rows
//                        val gridWidth = cellWidthPx * columns
//                        val gridHeight = cellHeightPx * rows
//
//                        // Draw vertical lines for the grid
//                        for (i in 0..columns) {
//                            val startX = i * cellWidthPx
//                            drawLine(
//                                color = gridColor,
//                                start = Offset(startX, 0f),
//                                end = Offset(startX, gridHeight),
//                                strokeWidth = lineThickness
//                            )
//                        }
//
//                        // Draw horizontal lines for the grid
//                        for (i in 0..rows) {
//                            val startY = i * cellHeightPx
//                            drawLine(
//                                color = gridColor,
//                                start = Offset(0f, startY),
//                                end = Offset(gridWidth, startY),
//                                strokeWidth = lineThickness
//                            )
//                        }
//                    }
//
//                    // Overlay text fields and handle clicks
//                    for (index in 0 until rows * columns) {
//                        val col = index % columns
//                        val row = index / columns
//
//                        // Box for each cell that can be clicked
//                        Box(
//                            modifier = Modifier
//                                .offset(
//                                    x = with(LocalDensity.current) { (col * cellWidth.toPx()).toDp() },
//                                    y = with(LocalDensity.current) { (row * cellHeight.toPx()).toDp() }
//                                )
//                                .clickable {
//                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
//                                        onCellClick(index)  // Handle empty cell click only if adding text is enabled
//                                    }
//                                },
//                            contentAlignment = Alignment.TopStart // Ensure top-left alignment
//                        ) {
//                            // Display the text field if it exists
//                            textFields[index]?.let { textField ->
//                                if (textField.text.isNotBlank()) {
//                                    // Let the text expand dynamically based on its content
//                                    val fontSize = textField.height.sp  // Text height as font size
//                                    val scaleX = textField.widthPercentage / 100 // Scale based on the width percentage
//
//                                    // Ensure the clickable area covers the entire text
//                                    Text(
//                                        text = textField.text,
//                                        fontSize = fontSize,
//                                        textAlign = TextAlign.Start,  // Align text to the start (left)
//                                        maxLines = 1,
//                                        modifier = Modifier
//                                            .wrapContentWidth(Alignment.Start) // Ensure the width expands to the right
//                                            .wrapContentHeight(Alignment.Top)  // Ensure the height expands downward
//                                            .scale(scaleX = scaleX, scaleY = 1f)
//                                            .clickable {
//                                                onTextClick(index, textField)  // Handle text field click
//                                            }
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}


//package com.tocho.mb3
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//@Composable
//fun GridComponent(
//    rows: Int = 20,          // Number of rows
//    columns: Int = 60,       // Number of columns
//    textFields: MutableMap<Int, MarkingItem>,  // Pass text fields for content
//    onCellClick: (Int) -> Unit,               // Handle cell click event
//    onTextClick: (Int, MarkingItem) -> Unit,  // Handle text click event
//    gridColor: Color = Color.LightGray,       // Customizable color for the grid
//    lineThickness: Float = 1f                 // Line thickness for grid lines
//) {
//    // State to track whether text-adding is enabled
//    var isTextAddingEnabled by remember { mutableStateOf(true) }
//
//    // Main layout with rows and columns
//    Column(
//        modifier = Modifier.fillMaxSize().padding(0.dp)  // No padding to keep buttons close to the edges
//    ) {
//        // Top row with buttons and grid numbers
//        Row(
//            verticalAlignment = Alignment.Top, // Align top buttons to the top of the screen
//            horizontalArrangement = Arrangement.Start  // Align leftmost button to the start (left)
//        ) {
//            // Top-left button (A) to toggle text-adding
//            Button(
//                onClick = {
//                    isTextAddingEnabled = !isTextAddingEnabled  // Toggle text-adding ability
//                },
//                modifier = Modifier.padding(0.dp)
//            ) {
//                Text(if (isTextAddingEnabled) "Disable Add Text" else "Enable Add Text")
//            }
//
//            // Send button to call sendTestDataToMB3 directly
//            Button(
//                onClick = { sendTestDataToMB3() },  // Directly calling sendTestDataToMB3
//                modifier = Modifier.padding(start = 4.dp)
//            ) {
//                Text("Send")
//            }
//
//            Button(onClick = { /* TODO: Action + */ }, modifier = Modifier.padding(start = 4.dp)) { Text("+") }
//            Button(onClick = { /* TODO: Action - */ }, modifier = Modifier.padding(start = 4.dp)) { Text("-") }
//        }
//
//        // Layout containing left buttons and grid
//        Row(modifier = Modifier.fillMaxSize()) {
//            // Left buttons (A, B, C, D) closely positioned to the left
//            Column(
//                modifier = Modifier.padding(top = 4.dp, start = 0.dp),  // Minimal padding to keep buttons close to the grid
//                verticalArrangement = Arrangement.spacedBy(4.dp)  // Minimal space between buttons
//            ) {
//                Button(onClick = { /* TODO: Action A */ }) { Text("B") }
//                Button(onClick = { /* TODO: Action B */ }) { Text("C") }
//                Button(onClick = { /* TODO: Action C */ }) { Text("D") }
//                Button(onClick = { /* TODO: Action D */ }) { Text("E") }
//            }
//
//            // Grid Layout
//            BoxWithConstraints(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(16.dp)  // Adjust padding around the whole grid and labels as needed
//            ) {
//                val cellWidth: Dp = maxWidth / columns
//                val cellHeight: Dp = maxHeight / rows
//
//                // Numbers on the top outside the grid
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(bottom = 8.dp),  // Add space between numbers and grid
//                    horizontalArrangement = Arrangement.SpaceBetween  // Evenly distribute numbers along the horizontal axis
//                ) {
//                    for (i in 0..columns) {  // Label every 5th column
//                        Text(
//                            text = "$i",
//                            fontSize = 5.sp,
//                            modifier = Modifier.padding(start = cellWidth / 2)  // Align number with the grid line
//                        )
//                    }
//                }
//
//                // Numbers on the left outside the grid
//                Column(
//                    modifier = Modifier
//                        .fillMaxHeight()
//                        .padding(end = 8.dp),  // Add space between numbers and grid
//                    verticalArrangement = Arrangement.SpaceBetween  // Evenly distribute numbers along the vertical axis
//                ) {
//                    for (i in 0..rows) {
//                        Text(
//                            text = "$i",  // Each row represents 5 mm
//                            fontSize = 5.sp,
//                            modifier = Modifier.padding(top = cellHeight / 2)  // Align number with the grid line
//                        )
//                    }
//                }
//
//                // Outer box containing the grid
//                Box(modifier = Modifier.fillMaxSize()) {
//                    // Canvas for drawing the grid itself
//                    Canvas(modifier = Modifier.fillMaxSize()) {
//                        val cellWidthPx = size.width / columns
//                        val cellHeightPx = size.height / rows
//                        val gridWidth = cellWidthPx * columns
//                        val gridHeight = cellHeightPx * rows
//
//                        // Draw vertical lines for the grid
//                        for (i in 0..columns) {
//                            val startX = i * cellWidthPx
//                            drawLine(
//                                color = gridColor,
//                                start = Offset(startX, 0f),
//                                end = Offset(startX, gridHeight),
//                                strokeWidth = lineThickness
//                            )
//                        }
//
//                        // Draw horizontal lines for the grid
//                        for (i in 0..rows) {
//                            val startY = i * cellHeightPx
//                            drawLine(
//                                color = gridColor,
//                                start = Offset(0f, startY),
//                                end = Offset(gridWidth, startY),
//                                strokeWidth = lineThickness
//                            )
//                        }
//                    }
//
//                    // Overlay text fields and handle clicks
//                    for (index in 0 until rows * columns) {
//                        val col = index % columns
//                        val row = index / columns
//
//                        // Box for each cell that can be clicked
//                        Box(
//                            modifier = Modifier
//                                .offset(
//                                    x = with(LocalDensity.current) { (col * cellWidth.toPx()).toDp() },
//                                    y = with(LocalDensity.current) { (row * cellHeight.toPx()).toDp() }
//                                )
//                                .clickable {
//                                    if (isTextAddingEnabled && !textFields.containsKey(index)) {
//                                        onCellClick(index)  // Handle empty cell click only if adding text is enabled
//                                    }
//                                },
//                            contentAlignment = Alignment.TopStart // Ensure top-left alignment
//                        ) {
//                            // Display the text field if it exists
//                            textFields[index]?.let { textField ->
//                                if (textField.text.isNotBlank()) {
//                                    // Let the text expand dynamically based on its content
//                                    val fontSize = textField.height.sp  // Text height as font size
//                                    val scaleX = textField.widthPercentage / 100 // Scale based on the width percentage
//
//                                    // Ensure the clickable area covers the entire text
//                                    Text(
//                                        text = textField.text,
//                                        fontSize = fontSize,
//                                        textAlign = TextAlign.Start,  // Align text to the start (left)
//                                        maxLines = 1,
//                                        modifier = Modifier
//                                            .wrapContentWidth(Alignment.Start) // Ensure the width expands to the right
//                                            .wrapContentHeight(Alignment.Top)  // Ensure the height expands downward
//                                            .scale(scaleX = scaleX, scaleY = 1f)
//                                            .clickable {
//                                                onTextClick(index, textField)  // Handle text field click
//                                            }
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

//HERE
//
//package com.tocho.mb3
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//@Composable
//fun GridComponent(
//    rows: Int = 20,          // Number of rows
//    columns: Int = 60,       // Number of columns
//    textFields: MutableMap<Int, MarkingItem>,  // Pass text fields for content
//    onCellClick: (Int) -> Unit,               // Handle cell click event
//    onTextClick: (Int, MarkingItem) -> Unit,  // Handle text click event
//    gridColor: Color = Color.LightGray,       // Customizable color for the grid
//    lineThickness: Float = 1f                 // Line thickness for grid lines
//) {
//    BoxWithConstraints(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(32.dp) // Padding around the whole grid and labels
//    ) {
//        val cellWidth: Dp = maxWidth / columns
//        val cellHeight: Dp = maxHeight / rows
//
//        Column(
//            modifier = Modifier
//                .fillMaxHeight()
//                .padding(end = 8.dp),  // Add space between numbers and grid
//            verticalArrangement = Arrangement.SpaceBetween  // Evenly distribute numbers along the vertical axis
//        ) {
//            for (i in 0..rows) {
//                Text(
//                    text = "$i",  // Each row represents 5 mm
//                    fontSize = 5.sp,
//                    modifier = Modifier.padding(top = cellHeight / 2)  // Align number with the grid line
//                )
//            }
//        }
//
//        // Row for displaying column numbers outside the grid
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 8.dp),  // Add space between numbers and grid
//            horizontalArrangement = Arrangement.SpaceBetween  // Evenly distribute numbers along the horizontal axis
//        ) {
//            for (i in 0..columns) {  // Label every 5th column
//                Text(
//                    text = "$i",
//                    fontSize = 5.sp,
//                    modifier = Modifier.padding(start = cellWidth / 2)  // Align number with the grid line
//                )
//            }
//        }
//
//        // Outer box containing the grid and the row/column numbers
//        Box(modifier = Modifier.fillMaxSize()) {
//            // Canvas for drawing the grid itself
//            Canvas(modifier = Modifier.fillMaxSize()) {
//                val cellWidthPx = size.width / columns
//                val cellHeightPx = size.height / rows
//                val gridWidth = cellWidthPx * columns
//                val gridHeight = cellHeightPx * rows
//
//                // Draw vertical lines for the grid
//                for (i in 0..columns) {
//                    val startX = i * cellWidthPx
//                    drawLine(
//                        color = gridColor,
//                        start = Offset(startX, 0f),
//                        end = Offset(startX, gridHeight),
//                        strokeWidth = lineThickness
//                    )
//                }
//
//                // Draw horizontal lines for the grid
//                for (i in 0..rows) {
//                    val startY = i * cellHeightPx
//                    drawLine(
//                        color = gridColor,
//                        start = Offset(0f, startY),
//                        end = Offset(gridWidth, startY),
//                        strokeWidth = lineThickness
//                    )
//                }
//            }
//
//            // Overlay text fields and handle clicks
//            for (index in 0 until rows * columns) {
//                val col = index % columns
//                val row = index / columns
//
//                // Box for each cell that can be clicked
//                Box(
//                    modifier = Modifier
//                        .offset(
//                            x = with(LocalDensity.current) { (col * cellWidth.toPx()).toDp() },
//                            y = with(LocalDensity.current) { (row * cellHeight.toPx()).toDp() }
//                        )
//                        .clickable {
//                            if (!textFields.containsKey(index)) {
//                                onCellClick(index)  // Handle empty cell click
//                            }
//                        },
//                    contentAlignment = Alignment.TopStart // Ensure top-left alignment
//                ) {
//                    // Display the text field if it exists
//                    textFields[index]?.let { textField ->
//                        if (textField.text.isNotBlank()) {
//                            // Let the text expand dynamically based on its content
//                            val fontSize = textField.height.sp  // Text height as font size
//                            val scaleX = textField.widthPercentage / 100 // Scale based on the width percentage
//
//                            // Calculate the width of the text in pixels to extend the clickable area
//                            val textWidth = with(LocalDensity.current) {
//                                fontSize.toPx() * textField.text.length * scaleX
//                            }
//
//                            // Make the entire text area clickable and not just within a grid cell
//                            Box(
//                                modifier = Modifier
//                                    .width(with(LocalDensity.current) { textWidth.toDp() })  // Adjust clickable width
//                                    .wrapContentHeight()  // Let the height wrap the content
//                                    .clickable {
//                                        onTextClick(index, textField)  // Handle text field click
//                                    }
//                            ) {
//                                Text(
//                                    text = textField.text,
//                                    fontSize = fontSize,
//                                    textAlign = TextAlign.Start,  // Align text to the start (left)
//                                    maxLines = 1,
//                                    modifier = Modifier
//                                        .scale(scaleX = scaleX, scaleY = 1f)  // Scale text width based on percentage
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

//package com.tocho.mb3
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//@Composable
//fun GridComponent(
//    rows: Int = 20,          // Number of rows
//    columns: Int = 60,       // Number of columns
//    textFields: MutableMap<Int, MarkingItem>,  // Pass text fields for content
//    onCellClick: (Int) -> Unit,               // Handle cell click event
//    onTextClick: (Int, MarkingItem) -> Unit,  // Handle text click event
//    gridColor: Color = Color.LightGray,       // Customizable color for the grid
//    lineThickness: Float = 1f                 // Line thickness for grid lines
//) {
//    BoxWithConstraints(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(32.dp) // Padding around the whole grid and labels
//    ) {
//        val cellWidth: Dp = maxWidth / columns
//        val cellHeight: Dp = maxHeight / rows
//
//        Column(
//            modifier = Modifier
//                .fillMaxHeight()
//                .padding(end = 8.dp),  // Add space between numbers and grid
//            verticalArrangement = Arrangement.SpaceBetween  // Evenly distribute numbers along the vertical axis
//        ) {
//            for (i in 0..rows) {
//                Text(
//                    text = "$i",  // Each row represents 5 mm
//                    fontSize = 5.sp,
//                    modifier = Modifier.padding(top = cellHeight / 2)  // Align number with the grid line
//                )
//            }
//        }
//
//        // Row for displaying column numbers outside the grid
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 8.dp),  // Add space between numbers and grid
//            horizontalArrangement = Arrangement.SpaceBetween  // Evenly distribute numbers along the horizontal axis
//        ) {
//            for (i in 0..columns) {  // Label every 5th column
//                Text(
//                    text = "$i",
//                    fontSize = 5.sp,
//                    modifier = Modifier.padding(start = cellWidth / 2)  // Align number with the grid line
//                )
//            }
//        }
//
//        // Outer box containing the grid and the row/column numbers
//        Box(modifier = Modifier.fillMaxSize()) {
//            // Canvas for drawing the grid itself
//            Canvas(modifier = Modifier.fillMaxSize()) {
//                val cellWidthPx = size.width / columns
//                val cellHeightPx = size.height / rows
//                val gridWidth = cellWidthPx * columns
//                val gridHeight = cellHeightPx * rows
//
//                // Draw vertical lines for the grid
//                for (i in 0..columns) {
//                    val startX = i * cellWidthPx
//                    drawLine(
//                        color = gridColor,
//                        start = Offset(startX, 0f),
//                        end = Offset(startX, gridHeight),
//                        strokeWidth = lineThickness
//                    )
//                }
//
//                // Draw horizontal lines for the grid
//                for (i in 0..rows) {
//                    val startY = i * cellHeightPx
//                    drawLine(
//                        color = gridColor,
//                        start = Offset(0f, startY),
//                        end = Offset(gridWidth, startY),
//                        strokeWidth = lineThickness
//                    )
//                }
//            }
//
//            // Overlay text fields and handle clicks
//            for (index in 0 until rows * columns) {
//                val col = index % columns
//                val row = index / columns
//
//                // Box for each cell that can be clicked
//                Box(
//                    modifier = Modifier
//                        .offset(
//                            x = with(LocalDensity.current) { (col * cellWidth.toPx()).toDp() },
//                            y = with(LocalDensity.current) { (row * cellHeight.toPx()).toDp() }
//                        )
//                        .clickable {
//                            if (!textFields.containsKey(index)) {
//                                onCellClick(index)  // Handle empty cell click
//                            }
//                        },
//                    contentAlignment = Alignment.TopStart // Ensure top-left alignment
//                ) {
//                    // Display the text field if it exists
//                    textFields[index]?.let { textField ->
//                        if (textField.text.isNotBlank()) {
//                            // Let the text expand dynamically based on its content
//                            val fontSize = textField.height.sp  // Text height as font size
//                            val scaleX = textField.widthPercentage / 100 // Scale based on the width percentage
//
//                            // Ensure the clickable area covers the entire text
//                            Text(
//                                text = textField.text,
//                                fontSize = fontSize,
//                                textAlign = TextAlign.Start,  // Align text to the start (left)
//                                maxLines = 1,
//                                modifier = Modifier
//                                    .wrapContentWidth(Alignment.Start) // Ensure the width expands to the right
//                                    .wrapContentHeight(Alignment.Top)  // Ensure the height expands downward
//                                    .scale(scaleX = scaleX, scaleY = 1f)
//                                    .clickable {
//                                        onTextClick(index, textField)  // Handle text field click
//                                    }
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}


//@Composable
//fun GridComponent(
//    rows: Int = 20,          // Number of rows
//    columns: Int = 60,       // Number of columns
//    textFields: MutableMap<Int, MarkingItem>,  // Pass text fields for content
//    onCellClick: (Int) -> Unit,               // Handle cell click event
//    onTextClick: (Int, MarkingItem) -> Unit,  // Handle text click event
//    gridColor: Color = Color.LightGray,       // Customizable color for the grid
//    lineThickness: Float = 1f                 // Line thickness for grid lines
//) {
//    BoxWithConstraints(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(32.dp) // Padding around the whole grid and labels
//    ) {
//        val cellWidth: Dp = maxWidth / columns
//        val cellHeight: Dp = maxHeight / rows
//
//        // Outer box containing the grid and the row/column numbers
//        Box(modifier = Modifier.fillMaxSize()) {
//            // Column for displaying row numbers outside the grid
//            Column(
//                modifier = Modifier
//                    .fillMaxHeight()
//                    .padding(end = 8.dp),  // Add space between numbers and grid
//                verticalArrangement = Arrangement.SpaceBetween  // Evenly distribute numbers along the vertical axis
//            ) {
//                for (i in 0..rows) {
//                    Text(
//                        text = "$i",  // Each row represents 5 mm
//                        fontSize = 5.sp,
//                        modifier = Modifier.padding(top = cellHeight / 2)  // Align number with the grid line
//                    )
//                }
//            }
//
//            // Row for displaying column numbers outside the grid
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 8.dp),  // Add space between numbers and grid
//                horizontalArrangement = Arrangement.SpaceBetween  // Evenly distribute numbers along the horizontal axis
//            ) {
//                for (i in 0..columns) {  // Label every 5th column
//                    Text(
//                        text = "$i",
//                        fontSize = 5.sp,
//                        modifier = Modifier.padding(start = cellWidth / 2)  // Align number with the grid line
//                    )
//                }
//            }
//
//            // Canvas for drawing the grid itself
//            Canvas(modifier = Modifier.fillMaxSize()) {
//                val cellWidthPx = size.width / columns
//                val cellHeightPx = size.height / rows
//                val gridWidth = cellWidthPx * columns
//                val gridHeight = cellHeightPx * rows
//
//                // Draw vertical lines for the grid
//                for (i in 0..columns) {
//                    val startX = i * cellWidthPx
//                    drawLine(
//                        color = gridColor,
//                        start = Offset(startX, 0f),
//                        end = Offset(startX, gridHeight),
//                        strokeWidth = lineThickness
//                    )
//                }
//
//                // Draw horizontal lines for the grid
//                for (i in 0..rows) {
//                    val startY = i * cellHeightPx
//                    drawLine(
//                        color = gridColor,
//                        start = Offset(0f, startY),
//                        end = Offset(gridWidth, startY),
//                        strokeWidth = lineThickness
//                    )
//                }
//            }
//
//            // Overlay text fields and handle clicks
//            for (index in 0 until rows * columns) {
//                val col = index % columns
//                val row = index / columns
//
//                // Box for each cell that can be clicked
//                Box(
//                    modifier = Modifier
//                        .offset(
//                            x = with(LocalDensity.current) { (col * cellWidth.toPx()).toDp() },
//                            y = with(LocalDensity.current) { (row * cellHeight.toPx()).toDp() }
//                        )
//                        .size(
//                            width = cellWidth,
//                            height = cellHeight
//                        )
//                        .clickable {
//                            if (!textFields.containsKey(index)) {
//                                onCellClick(index)  // Handle empty cell click
//                            }
//                        },
//                    contentAlignment = Alignment.TopStart
//                ) {
//                    // Display the text field if it exists
//                    textFields[index]?.let { textField ->
//                        if (textField.text.isNotBlank()) {
//                            // Let the text expand dynamically based on its content
//                            // Scale text width based on the percentage and adjust height accordingly
//                            val fontSize = textField.height.sp  // Text height as font size
//                            val scaleX = textField.widthPercentage / 100 // Scale based on the width percentage
//
//                            Text(
//                                text = textField.text,
//                                fontSize = fontSize,
////                                fontSize = textField.height.sp,  // Adjust the font size to be more visible
//                                textAlign = TextAlign.Start,
//                                maxLines = 1, // Set a max number of lines for the text
//                                modifier = Modifier
//                                    .padding(0.dp)
////                                    .padding(start = 0.dp)
////                                    .fillMaxWidth()
////                                    .wrapContentWidth(unbounded = true)
////                                    .wrapContentSize(Alignment.TopStart, unbounded = true)  // Ensure text expands to fit its content
//                                    .wrapContentHeight(Alignment.Top, unbounded = true)
//                                    .wrapContentWidth(Alignment.Start, unbounded = true)
//                                    .clickable {
//                                        onTextClick(index, textField)  // Handle text field click
//                                    }
//                                    .scale(scaleX = scaleX, scaleY = 1f)
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

//package com.tocho.mb3
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//@Composable
//fun GridComponent(
//    rows: Int = 20,          // Number of rows
//    columns: Int = 60,       // Number of columns
//    textFields: MutableMap<Int, MarkingItem>,  // Pass text fields for content
//    onCellClick: (Int) -> Unit,               // Handle cell click event
//    onTextClick: (Int, MarkingItem) -> Unit,  // Handle text click event
//    gridColor: Color = Color.LightGray,       // Customizable color for the grid
//    lineThickness: Float = 1f                 // Line thickness for grid lines
//) {
//    BoxWithConstraints(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(32.dp) // Padding around the whole grid and labels
//    ) {
//        val cellWidth: Dp = maxWidth / columns
//        val cellHeight: Dp = maxHeight / rows
//
//        // Outer box containing the grid and the row/column numbers
//        Box(modifier = Modifier.fillMaxSize()) {
//            // Column for displaying row numbers outside the grid
//            Column(
//                modifier = Modifier
//                    .fillMaxHeight()
//                    .padding(end = 8.dp),  // Add space between numbers and grid
//                verticalArrangement = Arrangement.SpaceBetween  // Evenly distribute numbers along the vertical axis
//            ) {
//                for (i in 0..rows) {
//                    Text(
//                        text = "$i",  // Each row represents 5 mm
//                        fontSize = 5.sp,
//                        modifier = Modifier.padding(top = cellHeight / 2)  // Align number with the grid line
//                    )
//                }
//            }
//
//            // Row for displaying column numbers outside the grid
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 8.dp),  // Add space between numbers and grid
//                horizontalArrangement = Arrangement.SpaceBetween  // Evenly distribute numbers along the horizontal axis
//            ) {
//                for (i in 0..columns) {  // Label every 5th column
//                    Text(
//                        text = "$i",
//                        fontSize = 5.sp,
//                        modifier = Modifier.padding(start = cellWidth / 2)  // Align number with the grid line
//                    )
//                }
//            }
//
//            // Canvas for drawing the grid itself
//            Canvas(modifier = Modifier.fillMaxSize()) {
//                val cellWidthPx = size.width / columns
//                val cellHeightPx = size.height / rows
//                val gridWidth = cellWidthPx * columns
//                val gridHeight = cellHeightPx * rows
//
//                // Draw vertical lines for the grid
//                for (i in 0..columns) {
//                    val startX = i * cellWidthPx
//                    drawLine(
//                        color = gridColor,
//                        start = Offset(startX, 0f),
//                        end = Offset(startX, gridHeight),
//                        strokeWidth = lineThickness
//                    )
//                }
//
//                // Draw horizontal lines for the grid
//                for (i in 0..rows) {
//                    val startY = i * cellHeightPx
//                    drawLine(
//                        color = gridColor,
//                        start = Offset(0f, startY),
//                        end = Offset(gridWidth, startY),
//                        strokeWidth = lineThickness
//                    )
//                }
//            }
//
//            // Overlay text fields and handle clicks
//            for (index in 0 until rows * columns) {
//                val col = index % columns
//                val row = index / columns
//
//                // Box for each cell that can be clicked
//                Box(
//                    modifier = Modifier
//                        .offset(
//                            x = with(LocalDensity.current) { (col * cellWidth.toPx()).toDp() },
//                            y = with(LocalDensity.current) { (row * cellHeight.toPx()).toDp() }
//                        )
//                        .size(
//                            width = cellWidth,
//                            height = cellHeight
//                        )
//                        .clickable {
//                            if (!textFields.containsKey(index)) {
//                                onCellClick(index)  // Handle empty cell click
//                            }
//                        },
//                    contentAlignment = Alignment.Center
//                ) {
//                    // Display the text field if it exists
//                    textFields[index]?.let { textField ->
//                        if (textField.text.isNotBlank()) {
//                            Text(
//                                text = textField.text,
//                                modifier = Modifier.clickable {
//                                    onTextClick(index, textField)  // Handle text field click
//                                }
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}


/*
package com.tocho.mb3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GridComponent(
    rows: Int = 20,          // Number of rows
    columns: Int = 60,       // Number of columns
    textFields: MutableMap<Int, MarkingItem>,  // Pass text fields for content
    onCellClick: (Int) -> Unit,               // Handle cell click event
    onTextClick: (Int, MarkingItem) -> Unit,  // Handle text click event
    gridColor: Color = Color.LightGray,       // Customizable color for the grid
    lineThickness: Float = 0.5f               // Thin grid lines to make the grid finer
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas for drawing grid, limited to the inside area
        // Calculate cellWidth and cellHeight based on the available space
        // Now that we're in the context of BoxWithConstraints, we can use maxWidth and maxHeight

        Canvas(modifier = Modifier
            .padding(start = 32.dp, top = 32.dp)  // Padding for row and column labels
            .fillMaxSize()) {

            val cellWidth = size.width / columns
            val cellHeight = size.height / rows
            val gridWidth = cellWidth * columns
            val gridHeight = cellHeight * rows

//            val cellWidth = size.width / columns
//            val cellHeight = size.height / rows
//            val gridWidth = cellWidth * columns
//            val gridHeight = cellHeight * rows

            // Draw vertical lines (including the left and right borders)
            for (i in 0..columns) {
                val startX = i * cellWidth
                drawLine(
                    color = if (i == 0 || i == columns) Color.Black else gridColor,  // Black for borders
                    start = Offset(startX, 0f),
                    end = Offset(startX, gridHeight),  // Limit lines within the grid
                    strokeWidth = lineThickness.dp.toPx()
                )
            }

            // Draw horizontal lines (including the top and bottom borders)
            for (i in 0..rows) {
                val startY = i * cellHeight
                drawLine(
                    color = if (i == 0 || i == rows) Color.Black else gridColor,  // Black for borders
                    start = Offset(0f, startY),
                    end = Offset(gridWidth, startY),  // Limit lines within the grid
                    strokeWidth = lineThickness.dp.toPx()
                )
            }
//            val cellWidth = size.width / columns
//            val cellHeight = size.height / rows
//
//            // Draw vertical lines inside the grid
//            for (i in 0..columns) {
//                val startX = i * cellWidth
//                drawLine(
//                    color = gridColor,
//                    start = Offset(startX, 0f),
//                    end = Offset(startX, rows * cellHeight),  // Limit lines within the grid
//                    strokeWidth = lineThickness.dp.toPx()
//                )
//            }
//
//            // Draw horizontal lines inside the grid
//            for (i in 0..rows) {
//                val startY = i * cellHeight
//                drawLine(
//                    color = gridColor,
//                    start = Offset(0f, startY),
//                    end = Offset(columns * cellWidth, startY),  // Limit lines within the grid
//                    strokeWidth = lineThickness.dp.toPx()
//                )
//            }
        }

        // Display row numbers (mm units) on the left side using Text
//        Column(
//            modifier = Modifier
//                .fillMaxHeight()
//                .padding(start = 4.dp, top = ( / 2).dp),  // Adjust 'top' padding to align numbers to the middle
//            verticalArrangement = Arrangement.SpaceEvenly  // Evenly distribute numbers along the vertical axis
//        ) {
//            for (i in 0 until rows) {
//                Text(
//                    text = "${i * 5}",  // Each row represents 5 mm
//                    fontSize = 10.sp,
//                    modifier = Modifier.padding(start = 4.dp)  // Adjust 'start' for horizontal spacing
//                )
//            }
//        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 24.dp, top = 32.dp),  // Add padding for better alignment
            verticalArrangement = Arrangement.Top
//            verticalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0 until rows) {
                Text(
                    text = "$i",  // Each row represents 5 mm
                    fontSize = 5.sp,
                    modifier = Modifier.padding(start = 0.dp)
                )
            }
        }

        // Display column numbers (mm units) at the top using Text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, top = 4.dp),  // Add padding for better alignment
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0 until columns) {  // Label every 5th column
                Text(
                    text = "$i",
                    fontSize = 5.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Overlay the text fields on top of the grid
        for (index in 0 until rows * columns) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .aspectRatio(1f)
                    .clickable {
                        if (!textFields.containsKey(index)) {
                            onCellClick(index)  // Handle empty cell click
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Display the text field if it exists, otherwise just show an empty cell
                textFields[index]?.let { textField ->
                    // Use Text instead of BasicText for grid overlay
                    if (textField.text.isNotBlank()) {
                        Text(
                            text = textField.text,  // This must be a String
                            modifier = Modifier.clickable {
                                onTextClick(index, textField)  // Handle text field click
                            }
                        )
                    }
                }
            }
        }
    }
}
*/