package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MeasurementEntity
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InteractiveChart(
    data: List<MeasurementEntity>,
    metricSelector: (MeasurementEntity) -> Float,
    metricName: String,
    metricUnit: String,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Interactive Viewport State
    var scaleX by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var selectedIndex by remember { mutableStateOf(-1) }

    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No historical telemetry data available.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
        return
    }

    // Prepare numerical coordinates
    val values = data.map(metricSelector)
    val maxVal = (values.maxOrNull() ?: 10f).coerceAtLeast(1f) * 1.15f
    val minVal = (values.minOrNull() ?: 0f).coerceAtLeast(0f) * 0.85f
    val valRange = (maxVal - minVal).coerceAtLeast(0.1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        // Chart Header and Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = metricName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Unit: $metricUnit | Range: ${String.format(Locale.US, "%.1f", minVal)}-${String.format(Locale.US, "%.1f", maxVal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Reset Zoom
                TextButton(
                    onClick = {
                        scaleX = 1f
                        offsetX = 0f
                        selectedIndex = -1
                    },
                    modifier = Modifier.testTag("reset_zoom_button")
                ) {
                    Text("Reset View", fontSize = 12.sp)
                }

                // Export PNG
                Button(
                    onClick = {
                        val path = exportChartAsPNG(data, values, metricName, metricUnit, lineColor)
                        if (path.isNotEmpty()) {
                            Toast.makeText(context, "Chart PNG saved to Downloads", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScadaBlueAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("export_png_button")
                ) {
                    Text("Export PNG", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Interactive Drawing Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scaleX = (scaleX * zoom).coerceIn(1f, 15f)
                        offsetX = (offsetX + pan.x).coerceIn(-1000f * scaleX, 1000f * scaleX)
                    }
                }
                .pointerInput(data.size) {
                    detectTapGestures { pressOffset ->
                        val itemWidth = size.width / (data.size - 1).coerceAtLeast(1)
                        val scaledWidth = itemWidth * scaleX
                        val startPos = offsetX
                        
                        var closestIndex = -1
                        var minDistance = Float.MAX_VALUE

                        for (i in data.indices) {
                            val cx = startPos + i * scaledWidth
                            val dist = Math.abs(cx - pressOffset.x)
                            if (dist < minDistance && dist < 48f) {
                                minDistance = dist
                                closestIndex = i
                            }
                        }
                        selectedIndex = closestIndex
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val gridLines = 5

                // Draw Horizontal Gridlines and Right Y-axis metrics
                val stepY = height / gridLines
                for (i in 0..gridLines) {
                    val y = i * stepY
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // Map data points into drawing viewport coordinates
                val pointsCount = data.size
                val segmentWidth = width / (pointsCount - 1).coerceAtLeast(1)
                val scaledSegmentWidth = segmentWidth * scaleX

                val coordinates = mutableListOf<Offset>()
                for (i in data.indices) {
                    val x = offsetX + i * scaledSegmentWidth
                    val relativeVal = (values[i] - minVal) / valRange
                    val y = height - (relativeVal * height)
                    coordinates.add(Offset(x, y))
                }

                // Draw solid line path
                if (coordinates.size > 1) {
                    val path = Path().apply {
                        moveTo(coordinates[0].x, coordinates[0].y)
                        for (i in 1 until coordinates.size) {
                            lineTo(coordinates[i].x, coordinates[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw filled Area gradient beneath the curve
                    val gradientPath = Path().apply {
                        moveTo(coordinates[0].x, height)
                        for (i in coordinates.indices) {
                            lineTo(coordinates[i].x, coordinates[i].y)
                        }
                        lineTo(coordinates.last().x, height)
                        close()
                    }
                    drawPath(
                        path = gradientPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )
                }

                // Draw highlight points and visual helper lines for tapped item
                if (selectedIndex in data.indices) {
                    val tappedPoint = coordinates[selectedIndex]
                    if (tappedPoint.x in 0f..width) {
                        // Vertical guideline
                        drawLine(
                            color = ScadaBlueAccent.copy(alpha = 0.6f),
                            start = Offset(tappedPoint.x, 0f),
                            end = Offset(tappedPoint.x, height),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )

                        // Outer ring highlight dot
                        drawCircle(
                            color = lineColor.copy(alpha = 0.4f),
                            radius = 8.dp.toPx(),
                            center = tappedPoint
                        )

                        // Inner solid dot
                        drawCircle(
                            color = lineColor,
                            radius = 4.dp.toPx(),
                            center = tappedPoint
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tooltip display box
        if (selectedIndex in data.indices) {
            val item = data[selectedIndex]
            val value = values[selectedIndex]
            val sdf = SimpleDateFormat("HH:mm:ss (MM-dd)", Locale.US)
            val formattedTime = sdf.format(Date(item.timestamp))

            Card(
                colors = CardDefaults.cardColors(containerColor = lineColor.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(lineColor, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Time: $formattedTime",
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${String.format(Locale.US, "%.2f", value)} $metricUnit",
                        fontSize = 13.sp,
                        color = lineColor,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        } else {
            // General prompt instruction when no point selected
            Text(
                "Tip: Pinch to ZOOM horizontally, drag to PAN, tap a point to show precise reading details.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// Function to export custom charts natively into a PNG file on external storage
fun exportChartAsPNG(
    data: List<MeasurementEntity>,
    values: List<Float>,
    metricName: String,
    metricUnit: String,
    lineColor: Color
): String {
    val width = 800
    val height = 450
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    // Paint configuration
    val paintBg = AndroidPaint().apply { color = android.graphics.Color.WHITE }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

    val paintGrid = AndroidPaint().apply {
        color = android.graphics.Color.LTGRAY
        strokeWidth = 1f
        style = AndroidPaint.Style.STROKE
    }

    // Grid lines
    val padding = 60f
    val chartWidth = width - 2 * padding
    val chartHeight = height - 2 * padding

    for (i in 0..4) {
        val y = padding + (chartHeight / 4) * i
        canvas.drawLine(padding, y, width - padding, y, paintGrid)
    }

    // Min and Max values
    val maxVal = (values.maxOrNull() ?: 10f).coerceAtLeast(1f) * 1.15f
    val minVal = (values.minOrNull() ?: 0f).coerceAtLeast(0f) * 0.85f
    val range = (maxVal - minVal).coerceAtLeast(0.1f)

    // Coordinates mapping
    val paintLine = AndroidPaint().apply {
        color = lineColor.toArgb()
        strokeWidth = 4f
        style = AndroidPaint.Style.STROKE
        isAntiAlias = true
    }

    val pointsCount = data.size
    val stepX = chartWidth / (pointsCount - 1).coerceAtLeast(1)

    var prevX = 0f
    var prevY = 0f
    for (i in data.indices) {
        val x = padding + i * stepX
        val y = padding + chartHeight - ((values[i] - minVal) / range) * chartHeight
        if (i > 0) {
            canvas.drawLine(prevX, prevY, x, y, paintLine)
        }
        prevX = x
        prevY = y
    }

    // Titles & Text labels
    val paintText = AndroidPaint().apply {
        color = android.graphics.Color.BLACK
        textSize = 24f
        isAntiAlias = true
    }
    canvas.drawText("$metricName Trend ($metricUnit)", padding, padding - 20f, paintText)

    val paintLabel = AndroidPaint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 14f
        isAntiAlias = true
    }
    canvas.drawText(String.format(Locale.US, "Max: %.1f", maxVal), width - padding - 100f, padding - 20f, paintLabel)
    canvas.drawText(String.format(Locale.US, "Min: %.1f", minVal), padding, height - padding + 30f, paintLabel)

    // Write file to device download directory
    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    val filename = "${metricName.replace(" ", "")}_Trend_${sdf.format(Date())}.png"
    
    try {
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!path.exists()) path.mkdirs()
        
        val file = File(path, filename)
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()
        return file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        return ""
    }
}
