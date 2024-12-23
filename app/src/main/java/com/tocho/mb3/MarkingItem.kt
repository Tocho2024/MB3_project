package com.tocho.mb3

// Data class to store properties of a text field (Marking)
data class MarkingItem(
    val text: String,  // Text content
    val x: Int,        // X position (in coordinate system)
    val y: Int,        // Y position (in coordinate system)
    val height: Int,   // Height of the text
    val angle: Float,  // Rotation angle
    val spacing: Float, // Character spacing
    val widthPercentage: Float, // Width of the text in percentage
    val force: Int,    // Marking force
    val quality: Int,  // Quality level
    val font: String = "F1"  // Font selection, default is "F1"
) {
    // Convert the object to a string for file storage
    override fun toString(): String {
        return "$text,$x,$y,$height,$angle,$spacing,$widthPercentage,$force,$quality,$font"
    }

    // Companion object to provide a method for parsing the string back into a MarkingItem
    companion object {
        fun fromString(data: String): MarkingItem? {
            return try {
                val parts = data.split(",")
                MarkingItem(
                    text = parts[0],
                    x = parts[1].toInt(),
                    y = parts[2].toInt(),
                    height = parts[3].toInt(),
                    angle = parts[4].toFloat(),
                    spacing = parts[5].toFloat(),
                    widthPercentage = parts[6].toFloat(),
                    force = parts[7].toInt(),
                    quality = parts[8].toInt(),
                    font = if (parts.size > 9) parts[9] else "F1"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        /*
        fun fromString(data: String): MarkingItem? {
            return try {
                val parts = data.split(",")
                if (parts.size < 9) {
                    println("Invalid data format: $data")
                    return null
                }
                MarkingItem(
                    text = parts[0],
                    x = parts[1].toIntOrNull() ?: 0,
                    y = parts[2].toIntOrNull() ?: 0,
                    height = parts[3].toIntOrNull() ?: 10,
                    angle = parts[4].toFloatOrNull() ?: 0f,
                    spacing = parts[5].toFloatOrNull() ?: 0f,
                    widthPercentage = parts[6].toFloatOrNull() ?: 100f,
                    force = parts[7].toIntOrNull() ?: 50,
                    quality = parts[8].toIntOrNull() ?: 100,
                    font = if (parts.size > 9) parts[9] else "F1"
                )
            } catch (e: Exception) {
                println("Error parsing data: ${e.message}")
                null
            }
        }
        */
    }
}

//package com.tocho.mb3
//
//// Data class to store properties of a text field (Marking)
//data class MarkingItem(
//    val text: String,  // Text content
//    val x: Int,        // X position (in coordinate system)
//    val y: Int,        // Y position (in coordinate system)
//    val height: Int,   // Height of the text
//    val angle: Float,  // Rotation angle
//    val spacing: Float, // Character spacing
//    val widthPercentage: Float, // Width of the text in percentage
//    val force: Int,    // Marking force
//    val quality: Int,  // Quality level
//    val font: String = "F1"  // Font selection, default is "F1"
//) {
//    // Convert the object to a string for file storage
//    override fun toString(): String {
//        return "$text,$x,$y,$height,$angle,$spacing,$widthPercentage,$force,$quality,$font"
//    }
//
//    // Companion object to provide a method for parsing the string back into a MarkingItem
//    companion object {
//        fun fromString(data: String): MarkingItem? {
//            return try {
//                val parts = data.split(",")
//                MarkingItem(
//                    text = parts[0],
//                    x = parts[1].toInt(),
//                    y = parts[2].toInt(),
//                    height = parts[3].toInt(),
//                    angle = parts[4].toFloat(),
//                    spacing = parts[5].toFloat(),
//                    widthPercentage = parts[6].toFloat(),
//                    force = parts[7].toInt(),
//                    quality = parts[8].toInt(),
//                    font = if (parts.size > 9) parts[9] else "F1"
//                )
//            } catch (e: Exception) {
//                e.printStackTrace()
//                null
//            }
//        }
//    }
//}