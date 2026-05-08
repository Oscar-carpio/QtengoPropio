package com.example.qtengo.familiar.ui.gastos

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Helper para exportar gastos a CSV o PDF y guardarlos en la carpeta Descargas.
// Usa MediaStore en Android 10+ y acceso directo al sistema de archivos en versiones anteriores.
object ExportarGastosHelper {

    // Genera un timestamp con formato yyyyMMdd_HHmm para el nombre del archivo
    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale("es", "ES")).format(Date())

    // ─── CSV ─────────────────────────────────────────────────────────────────

    // Exporta la lista de gastos a un archivo CSV con cabecera y resumen por categoría al final.
    // Las comas en la descripción se sustituyen por punto y coma para no romper el formato.
    fun exportarCSV(context: Context, gastos: List<Gasto>) {
        val fileName = "gastos_${timestamp()}.csv"
        val sb = StringBuilder()

        // Cabecera del CSV
        sb.appendLine("Fecha,Descripción,Categoría,Tipo,Cantidad (€)")
        gastos.forEach { g ->
            // Sustituimos comas en la descripción para no romper el formato CSV
            val descripcion = g.descripcion.replace(",", ";")
            sb.appendLine("${g.fecha},$descripcion,${g.categoria},${g.tipo},${g.cantidad}")
        }

        // Resumen por categoría al final del archivo
        sb.appendLine()
        sb.appendLine("RESUMEN POR CATEGORÍA")
        sb.appendLine("Categoría,Total (€)")
        gastos.filter { it.tipo == "GASTO" }
            .groupBy { it.categoria.ifBlank { "Sin categoría" } }
            .mapValues { (_, lista) -> lista.sumOf { it.cantidad } }
            .toSortedMap()
            .forEach { (cat, total) -> sb.appendLine("$cat,%.2f".format(total)) }

        guardarArchivo(context, fileName, "text/csv", sb.toString().toByteArray(Charsets.UTF_8), "CSV")
    }

    // ─── PDF ─────────────────────────────────────────────────────────────────

    // Exporta la lista de gastos a un PDF con cabecera, resumen general,
    // resumen por categoría y tabla de movimientos. Soporta múltiples páginas.
    fun exportarPDF(context: Context, gastos: List<Gasto>) {
        val fileName = "gastos_${timestamp()}.pdf"
        val document = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = 50f

        // Estilos de texto para los distintos elementos del PDF
        val paintTitulo = Paint().apply { textSize = 20f; isFakeBoldText = true; color = android.graphics.Color.rgb(26, 58, 107) }
        val paintSub = Paint().apply { textSize = 12f; color = android.graphics.Color.GRAY }
        val paintHeader = Paint().apply { textSize = 11f; isFakeBoldText = true; color = android.graphics.Color.rgb(26, 58, 107) }
        val paintBody = Paint().apply { textSize = 10f; color = android.graphics.Color.DKGRAY }
        val paintBodyAlt = Paint().apply { textSize = 10f; color = android.graphics.Color.rgb(60, 60, 60) }
        val paintLine = Paint().apply { strokeWidth = 0.8f; color = android.graphics.Color.LTGRAY; style = Paint.Style.STROKE }
        val paintSection = Paint().apply { textSize = 13f; isFakeBoldText = true; color = android.graphics.Color.rgb(26, 58, 107) }

        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(Date())
        val totalGastos = gastos.filter { it.tipo == "GASTO" }.sumOf { it.cantidad }
        val totalIngresos = gastos.filter { it.tipo == "INGRESO" }.sumOf { it.cantidad }

        // Crea una nueva página y reinicia el cursor vertical
        fun nuevaPagina() {
            document.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = 40f
        }

        // Comprueba si queda espacio suficiente; si no, crea una nueva página
        fun checkSalto(espacio: Float = 18f) {
            if (y + espacio > 820f) nuevaPagina()
        }

        // Encabezado del informe
        canvas.drawText("Informe de Gastos — QTengo", 40f, y, paintTitulo)
        y += 22f
        canvas.drawText("Generado el $fechaHoy · ${gastos.size} movimientos", 40f, y, paintSub)
        y += 25f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 20f

        // Sección resumen general: gastos, ingresos y balance
        canvas.drawText("RESUMEN", 40f, y, paintSection)
        y += 18f
        canvas.drawText("Total gastos:   %.2f€".format(totalGastos), 40f, y, paintBody)
        y += 16f
        canvas.drawText("Total ingresos: %.2f€".format(totalIngresos), 40f, y, paintBody)
        y += 16f
        canvas.drawText("Balance:        %.2f€".format(totalIngresos - totalGastos), 40f, y, paintBody)
        y += 25f

        // Sección resumen por categoría — solo se incluyen gastos, no ingresos
        canvas.drawText("POR CATEGORÍA", 40f, y, paintSection)
        y += 18f
        gastos.filter { it.tipo == "GASTO" }
            .groupBy { it.categoria.ifBlank { "Sin categoría" } }
            .mapValues { (_, lista) -> lista.sumOf { it.cantidad } }
            .toSortedMap()
            .forEach { (cat, total) ->
                checkSalto()
                canvas.drawText("• $cat", 50f, y, paintBody)
                canvas.drawText("%.2f€".format(total), 480f, y, paintBody)
                y += 16f
            }
        y += 20f

        // Sección tabla de movimientos
        checkSalto(40f)
        canvas.drawText("MOVIMIENTOS", 40f, y, paintSection)
        y += 18f

        // Cabecera de la tabla
        canvas.drawText("Fecha", 40f, y, paintHeader)
        canvas.drawText("Descripción", 115f, y, paintHeader)
        canvas.drawText("Categoría", 320f, y, paintHeader)
        canvas.drawText("Tipo", 435f, y, paintHeader)
        canvas.drawText("Cantidad", 490f, y, paintHeader)
        y += 6f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 14f

        // Filas alternadas con dos tonos de gris para mejorar la legibilidad
        gastos.forEachIndexed { i, g ->
            checkSalto()
            val paint = if (i % 2 == 0) paintBody else paintBodyAlt
            canvas.drawText(g.fecha, 40f, y, paint)
            canvas.drawText(g.descripcion.take(26), 115f, y, paint) // Truncamos para no salir del margen
            canvas.drawText(g.categoria.take(16), 320f, y, paint)
            canvas.drawText(g.tipo, 435f, y, paint)
            canvas.drawText("%.2f€".format(g.cantidad), 490f, y, paint)
            y += 16f
        }

        document.finishPage(page)

        // Convertimos el documento a bytes y lo guardamos en Descargas
        val bytes = ByteArrayOutputStream().also { document.writeTo(it); document.close() }.toByteArray()
        guardarArchivo(context, fileName, "application/pdf", bytes, "PDF")
    }

    // ─── Guardar en Descargas ─────────────────────────────────────────────────

    // Guarda el archivo en la carpeta Descargas del dispositivo.
    // Android 10+: usa MediaStore para respetar el almacenamiento por ámbitos.
    // Android 9 e inferior: escribe directamente en el directorio público de descargas.
    private fun guardarArchivo(context: Context, fileName: String, mimeType: String, data: ByteArray, tipo: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    // IS_PENDING = 1 reserva el archivo mientras se escribe — se libera al terminar
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw Exception("No se pudo crear el archivo")
                context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } else {
                // En Android 9 e inferior escribimos directamente en el directorio público
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(dir, fileName).writeBytes(data)
            }
            Toast.makeText(context, "$tipo guardado en Descargas: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error al exportar $tipo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}