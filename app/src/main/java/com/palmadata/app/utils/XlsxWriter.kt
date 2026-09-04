package com.palmadata.app.utils

import java.io.BufferedOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Escribe un libro .xlsx con UNA hoja, sin librerías externas.
 *
 * Un .xlsx es un ZIP con unos pocos XML; para exportar tablas planas no hace
 * falta Apache POI (10 MB de APK) ni nada parecido. Se genera lo mínimo que
 * Excel, LibreOffice y openpyxl aceptan sin quejarse:
 *
 *   [Content_Types].xml          tipos de cada parte
 *   _rels/.rels                  raíz -> workbook
 *   xl/workbook.xml              lista de hojas
 *   xl/_rels/workbook.xml.rels   workbook -> hoja y estilos
 *   xl/styles.xml                estilos mínimos (Excel exige la parte)
 *   xl/worksheets/sheet1.xml     los datos
 *
 * Los textos van como "inlineStr" (dentro de la propia celda) para no tener
 * que construir la tabla de cadenas compartidas. Los números van como celdas
 * numéricas de verdad, así el pipeline los lee tipados y no como texto.
 *
 * La hoja se escribe en streaming fila a fila sobre el ZIP: nunca se arma el
 * XML completo en memoria, así que sirve igual para 50 filas que para los
 * miles de tracks de una jornada.
 */
object XlsxWriter {

    /**
     * @param salida   destino. NO se cierra aquí: quien lo abrió lo cierra (así
     *                 puede hacer fsync antes de cerrar). Se hace finish() del
     *                 ZIP, que deja el archivo completo y válido.
     * @param hoja     nombre de la hoja (máx. 31 caracteres, regla de Excel)
     * @param columnas encabezados, en el orden en que irán las columnas
     * @param filas    cada fila es un mapa columna -> valor; las columnas que
     *                 falten en una fila quedan vacías
     */
    fun escribir(
        salida: OutputStream,
        hoja: String,
        columnas: List<String>,
        filas: List<Map<String, Any?>>
    ) {
        val zip = ZipOutputStream(BufferedOutputStream(salida))
        run {
            parte(zip, "[Content_Types].xml", CONTENT_TYPES)
            parte(zip, "_rels/.rels", RELS)
            parte(zip, "xl/workbook.xml", workbook(hoja))
            parte(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            parte(zip, "xl/styles.xml", STYLES)

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            // OJO: no se hace .close() sobre este writer, cerraría el ZIP entero.
            // Solo flush() al terminar la entrada.
            val w = OutputStreamWriter(zip, Charsets.UTF_8)
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            w.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
            w.write("<sheetData>")

            // Fila 1: encabezados (en negrita, estilo 1)
            w.write("<row r=\"1\">")
            columnas.forEachIndexed { i, nombre ->
                w.write("<c r=\"${letra(i)}1\" s=\"1\" t=\"inlineStr\"><is><t>${xml(nombre)}</t></is></c>")
            }
            w.write("</row>")

            // Datos desde la fila 2
            filas.forEachIndexed { idx, fila ->
                val r = idx + 2
                w.write("<row r=\"$r\">")
                columnas.forEachIndexed { i, col ->
                    celda(w, "${letra(i)}$r", fila[col])
                }
                w.write("</row>")
            }

            w.write("</sheetData></worksheet>")
            w.flush()
            zip.closeEntry()
        }
        // finish() escribe el directorio central del ZIP sin cerrar `salida`.
        zip.finish()
        zip.flush()
    }

    // ── Celdas ────────────────────────────────────────────────────────────────

    private fun celda(w: OutputStreamWriter, ref: String, valor: Any?) {
        when (valor) {
            // NULL de SQLite viaja como JSONObject.NULL en los mapas de la app.
            null, org.json.JSONObject.NULL -> return  // celda vacía: no se emite
            is Int, is Long, is Short, is Byte ->
                w.write("<c r=\"$ref\"><v>$valor</v></c>")
            is Double, is Float -> {
                val d = (valor as Number).toDouble()
                // NaN/Infinito no son válidos en XLSX: se dejan como texto
                if (d.isNaN() || d.isInfinite()) {
                    w.write("<c r=\"$ref\" t=\"inlineStr\"><is><t>${xml(valor.toString())}</t></is></c>")
                } else {
                    w.write("<c r=\"$ref\"><v>$d</v></c>")
                }
            }
            is Boolean ->
                w.write("<c r=\"$ref\" t=\"b\"><v>${if (valor) 1 else 0}</v></c>")
            else ->
                w.write("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(valor.toString())}</t></is></c>")
        }
    }

    /** 0 -> A, 25 -> Z, 26 -> AA ... */
    internal fun letra(indice: Int): String {
        var n = indice
        val sb = StringBuilder()
        while (true) {
            sb.insert(0, ('A' + n % 26))
            n = n / 26 - 1
            if (n < 0) break
        }
        return sb.toString()
    }

    private fun xml(s: String): String {
        val sb = StringBuilder(s.length + 16)
        for (ch in s) {
            when (ch) {
                '&'  -> sb.append("&amp;")
                '<'  -> sb.append("&lt;")
                '>'  -> sb.append("&gt;")
                '"'  -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                // Caracteres de control no permitidos en XML 1.0 (salvo tab, LF, CR)
                else -> if (ch.code < 0x20 && ch != '\t' && ch != '\n' && ch != '\r') sb.append(' ') else sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun parte(zip: ZipOutputStream, nombre: String, contenido: String) {
        zip.putNextEntry(ZipEntry(nombre))
        zip.write(contenido.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    // ── Partes fijas ─────────────────────────────────────────────────────────

    private fun workbook(hoja: String): String {
        val nombre = xml(hoja.take(31).replace(Regex("[\\\\/*?:\\[\\]]"), "_"))
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets><sheet name=\"$nombre\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
                "</workbook>"
    }

    private const val CONTENT_TYPES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
                "</Types>"

    private const val RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "</Relationships>"

    private const val WORKBOOK_RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
                "</Relationships>"

    // Estilo 0: normal. Estilo 1: negrita (encabezados).
    private const val STYLES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
                "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
                "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>" +
                "<fill><patternFill patternType=\"gray125\"/></fill></fills>" +
                "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
                "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
                "<cellXfs count=\"2\">" +
                "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
                "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
                "</cellXfs>" +
                "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
                "</styleSheet>"
}