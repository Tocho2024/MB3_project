package com.tocho.mb3

import java.io.File
import java.io.OutputStream

// Class or object to handle machine communication
object MachineCommunication {

    // Function to generate STM data from your grid
    fun generateSTMFileContent(gridData: List<GridTextItem>): String {
        // Create the STM format from the grid data
        val stringBuilder = StringBuilder()

        // Example: Add header or metadata if needed
        stringBuilder.append("1:FILE\\000.txt\n")
        stringBuilder.append("//\n")

        // Generate text objects in STM format
        gridData.forEach { item ->
            stringBuilder.append(
                "TEXT,F1,H${item.height},W${item.width},x${item.x},y${item.y},A${item.angle},p${item.pitch},f${item.force},s${item.speed},\"${item.text}\"\n"
            )
        }

        return stringBuilder.toString()
    }

    // Function to send the STM file to the machine via an OutputStream (network or file)
    fun sendSTMData(outputStream: OutputStream, stmData: String) {
        outputStream.write(stmData.toByteArray())
        outputStream.flush()
        outputStream.close()
    }
}

// A data class to represent each text item from the grid
data class GridTextItem(
    val height: Float,
    val width: Float,
    val x: Float,
    val y: Float,
    val angle: Float,
    val pitch: Float,
    val force: Int,
    val speed: Int,
    val text: String
)
