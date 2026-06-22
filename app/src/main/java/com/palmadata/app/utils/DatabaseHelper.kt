package com.palmadata.app.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME    = "palma_data.db"
        const val DB_VERSION = 12

        const val T_PLANTACIONES       = "plantaciones"
        const val T_TRABAJADORES       = "trabajadores"
        const val T_SECTORES           = "sectores"
        const val T_LOTES              = "lotes"
        const val T_ENFERMEDADES       = "enfermedades"
        const val T_EVENTOS            = "eventos"
        const val T_TRATAMIENTOS_EVT   = "tratamientos_eventos"
        const val T_CENSO_ENF          = "censo_enfermedades"
        const val T_TRATAMIENTOS       = "tratamientos"
        const val T_POLINIZACION       = "polinizacion"
        const val T_POLEN              = "polen_inicial_final"
        const val T_STRATEGUS          = "sanstrategus"
        const val T_TRAMPAS_MAESTRO    = "trampas"
        const val T_TRAMPAS            = "censo_trampas"
        const val T_INSECTOS           = "insectos"
        const val T_ESTADOS_INSECTO    = "estados_insecto"
        const val T_PLAGAS             = "muestreo_plagas"
        const val T_SUPER_COSECHA      = "super_cosecha"
        const val T_MAQUINARIA_MAESTRO = "maquinaria_maestro"
        const val T_IMPLEMENTOS        = "implementos"
        const val T_LABORES_MAQUINARIA = "labores_maquinaria"
        const val T_UNIDADES_MAQUINARIA= "unidades_maquinaria"
        const val T_MAQUINARIA_SESION  = "maquinaria_sesion"
        const val T_TRACKS             = "tracks_movil"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $T_PLANTACIONES (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL)")
        db.execSQL("CREATE TABLE $T_TRABAJADORES (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL, supervisor INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE $T_SECTORES (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL, plantacion_id INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE $T_LOTES (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL, sector_id INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE $T_ENFERMEDADES (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL)")
        db.execSQL("CREATE TABLE $T_EVENTOS (id INTEGER PRIMARY KEY, codigo TEXT NOT NULL, enfermedad_id INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE $T_TRATAMIENTOS_EVT (id INTEGER PRIMARY KEY, codigo TEXT NOT NULL)")
        db.execSQL("CREATE TABLE $T_TRAMPAS_MAESTRO (id INTEGER PRIMARY KEY, codigo TEXT NOT NULL)")
        db.execSQL("CREATE TABLE $T_INSECTOS (id INTEGER PRIMARY KEY, insecto TEXT NOT NULL)")
        db.execSQL("CREATE TABLE $T_ESTADOS_INSECTO (id INTEGER PRIMARY KEY, estado TEXT NOT NULL, insecto_id INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE $T_MAQUINARIA_MAESTRO (id INTEGER PRIMARY KEY, descripcion TEXT NOT NULL)")
        db.execSQL("CREATE TABLE $T_IMPLEMENTOS (id INTEGER PRIMARY KEY, descripcion TEXT NOT NULL)")
        db.execSQL("CREATE TABLE $T_LABORES_MAQUINARIA (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL)")
        db.execSQL("CREATE TABLE $T_UNIDADES_MAQUINARIA (id INTEGER PRIMARY KEY, descripcion TEXT NOT NULL)")
        db.execSQL("""CREATE TABLE $T_TRACKS (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            idunico TEXT NOT NULL,
            x REAL NOT NULL,
            y REAL NOT NULL,
            velocidad REAL DEFAULT 0,
            precision REAL DEFAULT 0,
            sentido REAL DEFAULT 0,
            proveedor TEXT,
            fecha TEXT NOT NULL,
            hora TEXT NOT NULL,
            trabajador INTEGER DEFAULT 0,
            plantacion_id INTEGER DEFAULT 0,
            formulario INTEGER DEFAULT 0,
            equipo TEXT,
            maquina INTEGER DEFAULT 0,
            labormaquina INTEGER DEFAULT 0,
            lote_id INTEGER DEFAULT 0,
            procesado INTEGER DEFAULT 0,
            sesionmaquinaria TEXT,
            sincronizado INTEGER DEFAULT 0
        )""")
        db.execSQL("""CREATE TABLE $T_CENSO_ENF (id TEXT PRIMARY KEY, censo INTEGER NOT NULL, fecha TEXT NOT NULL, hora TEXT NOT NULL, evaluador INTEGER NOT NULL, san_evento_enf_id INTEGER NOT NULL, san_enfermedades_id INTEGER NOT NULL, observaciones TEXT, linea INTEGER NOT NULL, palma INTEGER NOT NULL, cat_lote_id INTEGER NOT NULL, cat_palma_id INTEGER DEFAULT 0, cat_plantacion_id INTEGER NOT NULL, latitud REAL NOT NULL, longitud REAL NOT NULL, equipo TEXT NOT NULL, sincronizado INTEGER DEFAULT 0)""")
        db.execSQL("""CREATE TABLE $T_TRATAMIENTOS (id TEXT PRIMARY KEY, san_evento_trat_id INTEGER NOT NULL, aux_trabajador_id INTEGER NOT NULL, fecha TEXT NOT NULL, hora TEXT NOT NULL, cat_lote_id INTEGER NOT NULL, cat_palma_id REAL DEFAULT 0, cat_plantacion_id INTEGER DEFAULT 0, linea INTEGER NOT NULL, palma INTEGER NOT NULL, san_enfermedades_id INTEGER NOT NULL, san_evento_enf_id INTEGER NOT NULL, observaciones TEXT, latitud REAL NOT NULL, longitud REAL NOT NULL, cantidad REAL DEFAULT 0, equipo TEXT NOT NULL, sincronizado INTEGER DEFAULT 0)""")
        db.execSQL("""CREATE TABLE $T_POLINIZACION (id TEXT PRIMARY KEY, fecha TEXT NOT NULL, hora TEXT NOT NULL, linea INTEGER NOT NULL, palma INTEGER NOT NULL, cat_lote_id INTEGER NOT NULL, cat_palma_id INTEGER DEFAULT 0, cat_plantacion_id INTEGER NOT NULL, polinizador INTEGER NOT NULL, aplicacion1 INTEGER DEFAULT 0, aplicacion2 INTEGER DEFAULT 0, aplicacion3 INTEGER DEFAULT 0, observaciones TEXT, latitud REAL NOT NULL, longitud REAL NOT NULL, equipo TEXT NOT NULL, sincronizado INTEGER DEFAULT 0)""")
        db.execSQL("""CREATE TABLE $T_POLEN (id INTEGER PRIMARY KEY AUTOINCREMENT, fecha TEXT NOT NULL, inicial REAL DEFAULT 0, final REAL DEFAULT 0, trabajador INTEGER NOT NULL, sincronizado INTEGER DEFAULT 0)""")
        db.execSQL("""CREATE TABLE $T_STRATEGUS (id TEXT PRIMARY KEY, fecha TEXT NOT NULL, hora TEXT NOT NULL, cat_lote_id INTEGER NOT NULL, linea INTEGER NOT NULL, palma INTEGER NOT NULL, cat_palma_id INTEGER DEFAULT 0, galerias INTEGER DEFAULT 0, censo INTEGER NOT NULL, evaluador INTEGER NOT NULL, cat_plantacion_id INTEGER NOT NULL, observaciones TEXT, latitud REAL NOT NULL, longitud REAL NOT NULL, equipo TEXT NOT NULL, sincronizado INTEGER DEFAULT 0)""")
        db.execSQL("""CREATE TABLE $T_TRAMPAS (id TEXT PRIMARY KEY, fecha TEXT NOT NULL, hora TEXT NOT NULL, lectura INTEGER NOT NULL, censador INTEGER NOT NULL, machos INTEGER DEFAULT 0, hembras INTEGER DEFAULT 0, san_trampa_id INTEGER NOT NULL, san_tipo_trampa INTEGER DEFAULT 0, cat_plantacion_id INTEGER NOT NULL, atrayente INTEGER DEFAULT 0, feromona TEXT, observaciones TEXT, equipo TEXT NOT NULL, sincronizado INTEGER DEFAULT 0)""")
        db.execSQL("""CREATE TABLE $T_PLAGAS (id TEXT PRIMARY KEY, fecha TEXT NOT NULL, hora TEXT NOT NULL, lectura INTEGER DEFAULT 0, linea INTEGER DEFAULT 0, palma INTEGER DEFAULT 0, cat_lote_id INTEGER NOT NULL, cat_palma_id INTEGER DEFAULT 0, cat_plantacion_id INTEGER NOT NULL, evaluador INTEGER NOT NULL, insecto_id INTEGER DEFAULT 0, estado_insecto_id INTEGER DEFAULT 0, cantidad INTEGER DEFAULT 0, niv_foliar INTEGER DEFAULT 0, defol5 REAL DEFAULT 0, defol13 REAL DEFAULT 0, defol21 REAL DEFAULT 0, defol29 REAL DEFAULT 0, defol37 REAL DEFAULT 0, observaciones TEXT, latitud REAL NOT NULL, longitud REAL NOT NULL, equipo TEXT NOT NULL, sincronizado INTEGER DEFAULT 0)""")
        db.execSQL("""CREATE TABLE $T_SUPER_COSECHA (id_unico TEXT PRIMARY KEY, fecha TEXT NOT NULL, hora TEXT NOT NULL, supervisor INTEGER NOT NULL, cortador INTEGER DEFAULT 0, recolector INTEGER DEFAULT 0, linea INTEGER DEFAULT 0, palma INTEGER DEFAULT 0, ciclo INTEGER DEFAULT 0, cat_lote_id INTEGER NOT NULL, cat_plantacion_id INTEGER NOT NULL, racimos_recogidos INTEGER DEFAULT 0, racimos_verdes INTEGER DEFAULT 0, racimos_sobremaduros INTEGER DEFAULT 0, racimos_podridos INTEGER DEFAULT 0, racimossinrecoger INTEGER DEFAULT 0, racimossincortar INTEGER DEFAULT 0, racimorobado INTEGER DEFAULT 0, hojasmalacomo INTEGER DEFAULT 0, hojacolgando INTEGER DEFAULT 0, frutoplato INTEGER DEFAULT 0, observaciones TEXT, latitud REAL NOT NULL, longitud REAL NOT NULL, equipo TEXT NOT NULL, sincronizado INTEGER DEFAULT 0)""")
        db.execSQL("""CREATE TABLE $T_MAQUINARIA_SESION (id_unico TEXT PRIMARY KEY, maquina INTEGER DEFAULT 0, plantacion INTEGER DEFAULT 0, implemento INTEGER DEFAULT 0, labor INTEGER DEFAULT 0, trabajador INTEGER DEFAULT 0, kiloinicial REAL DEFAULT 0, kilofinal REAL DEFAULT 0, combustible REAL DEFAULT 0, horometroinicial REAL DEFAULT 0, horometrofinal REAL DEFAULT 0, lote TEXT, observaciones TEXT, unidadcantidad INTEGER DEFAULT 0, cantidad REAL DEFAULT 0, fechainicial TEXT, horainicial TEXT, fechafinal TEXT, horafinal TEXT, equipo TEXT, sincronizado INTEGER DEFAULT 0)""")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        listOf(T_PLANTACIONES, T_TRABAJADORES, T_SECTORES, T_LOTES,
            T_ENFERMEDADES, T_EVENTOS, T_TRATAMIENTOS_EVT,
            T_CENSO_ENF, T_TRATAMIENTOS, T_POLINIZACION,
            T_POLEN, T_STRATEGUS, T_TRAMPAS_MAESTRO, T_TRAMPAS,
            T_INSECTOS, T_ESTADOS_INSECTO, T_PLAGAS, T_SUPER_COSECHA,
            T_MAQUINARIA_MAESTRO, T_IMPLEMENTOS, T_LABORES_MAQUINARIA,
            T_UNIDADES_MAQUINARIA, T_MAQUINARIA_SESION, T_TRACKS).forEach {
            db.execSQL("DROP TABLE IF EXISTS $it")
        }
        onCreate(db)
    }

    // ── Reemplazar maestros ───────────────────────────────────────────────────

    fun reemplazarPlantaciones(lista: List<Pair<Int, String>>) = reemplazarPares(T_PLANTACIONES, lista)
    fun reemplazarEnfermedades(lista: List<Pair<Int, String>>) = reemplazarPares(T_ENFERMEDADES, lista)

    fun reemplazarTrabajadores(lista: List<Triple<Int, String, Int>>) {
        val db = writableDatabase; db.beginTransaction()
        try {
            db.delete(T_TRABAJADORES, null, null)
            lista.forEach { (id, nombre, supervisor) ->
                db.insert(T_TRABAJADORES, null, ContentValues().apply {
                    put("id", id); put("nombre", nombre); put("supervisor", supervisor)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun reemplazarSectores(lista: List<Triple<Int, String, Int>>) {
        val db = writableDatabase; db.beginTransaction()
        try { db.delete(T_SECTORES, null, null); lista.forEach { (id, nombre, plantacionId) -> db.insert(T_SECTORES, null, ContentValues().apply { put("id", id); put("nombre", nombre); put("plantacion_id", plantacionId) }) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    fun reemplazarLotes(lista: List<Triple<Int, String, Int>>) {
        val db = writableDatabase; db.beginTransaction()
        try { db.delete(T_LOTES, null, null); lista.forEach { (id, nombre, sectorId) -> db.insert(T_LOTES, null, ContentValues().apply { put("id", id); put("nombre", nombre); put("sector_id", sectorId) }) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    fun reemplazarEventos(lista: List<Triple<Int, String, Int>>) {
        val db = writableDatabase; db.beginTransaction()
        try { db.delete(T_EVENTOS, null, null); lista.forEach { (id, codigo, enfId) -> db.insert(T_EVENTOS, null, ContentValues().apply { put("id", id); put("codigo", codigo); put("enfermedad_id", enfId) }) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    fun reemplazarTratamientosEventos(lista: List<Pair<Int, String>>) {
        val db = writableDatabase; db.beginTransaction()
        try { db.delete(T_TRATAMIENTOS_EVT, null, null); lista.forEach { (id, codigo) -> db.insert(T_TRATAMIENTOS_EVT, null, ContentValues().apply { put("id", id); put("codigo", codigo) }) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    fun reemplazarTrampas(lista: List<Pair<Int, String>>) {
        val db = writableDatabase; db.beginTransaction()
        try { db.delete(T_TRAMPAS_MAESTRO, null, null); lista.forEach { (id, codigo) -> db.insert(T_TRAMPAS_MAESTRO, null, ContentValues().apply { put("id", id); put("codigo", codigo) }) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    fun reemplazarInsectos(lista: List<Pair<Int, String>>) {
        val db = writableDatabase; db.beginTransaction()
        try { db.delete(T_INSECTOS, null, null); lista.forEach { (id, insecto) -> db.insert(T_INSECTOS, null, ContentValues().apply { put("id", id); put("insecto", insecto) }) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    fun reemplazarEstadosInsecto(lista: List<Triple<Int, String, Int>>) {
        val db = writableDatabase; db.beginTransaction()
        try { db.delete(T_ESTADOS_INSECTO, null, null); lista.forEach { (id, estado, insectoId) -> db.insert(T_ESTADOS_INSECTO, null, ContentValues().apply { put("id", id); put("estado", estado); put("insecto_id", insectoId) }) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    fun reemplazarMaquinaria(lista: List<Pair<Int, String>>) {
        val db = writableDatabase; db.beginTransaction()
        try { db.delete(T_MAQUINARIA_MAESTRO, null, null); lista.forEach { (id, descripcion) -> db.insert(T_MAQUINARIA_MAESTRO, null, ContentValues().apply { put("id", id); put("descripcion", descripcion) }) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    fun reemplazarImplementos(lista: List<Pair<Int, String>>) {
        val db = writableDatabase; db.beginTransaction()
        try { db.delete(T_IMPLEMENTOS, null, null); lista.forEach { (id, descripcion) -> db.insert(T_IMPLEMENTOS, null, ContentValues().apply { put("id", id); put("descripcion", descripcion) }) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    fun reemplazarLaboresMaquinaria(lista: List<Pair<Int, String>>) {
        val db = writableDatabase; db.beginTransaction()
        try { db.delete(T_LABORES_MAQUINARIA, null, null); lista.forEach { (id, nombre) -> db.insert(T_LABORES_MAQUINARIA, null, ContentValues().apply { put("id", id); put("nombre", nombre) }) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    fun reemplazarUnidadesMaquinaria(lista: List<Pair<Int, String>>) {
        val db = writableDatabase; db.beginTransaction()
        try { db.delete(T_UNIDADES_MAQUINARIA, null, null); lista.forEach { (id, descripcion) -> db.insert(T_UNIDADES_MAQUINARIA, null, ContentValues().apply { put("id", id); put("descripcion", descripcion) }) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    // ── Tracks movil ──────────────────────────────────────────────────────────

    fun guardarTrack(track: com.palmadata.app.data.model.TrackMovil) {
        writableDatabase.insert(T_TRACKS, null, ContentValues().apply {
            put("idunico",         track.idunico)
            put("x",               track.x)
            put("y",               track.y)
            put("velocidad",       track.velocidad)
            put("precision",       track.precision)
            put("sentido",         track.sentido)
            put("proveedor",       track.proveedor)
            put("fecha",           track.fecha)
            put("hora",            track.hora)
            put("trabajador",      track.trabajador)
            put("plantacion_id",   track.plantacionId)
            put("formulario",      track.formulario)
            put("equipo",          track.equipo)
            put("maquina",         track.maquina)
            put("labormaquina",    track.laborMaquina)
            put("lote_id",         track.loteId)
            put("procesado",       track.procesado)
            put("sesionmaquinaria",track.sesionMaquinaria)
            put("sincronizado",    0)
        })
    }

    fun getTracksPendientes(): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        val cursor = readableDatabase.query(T_TRACKS, null, "sincronizado = 0", null, null, null, "id ASC")
        cursor.use {
            while (it.moveToNext()) {
                val map = mutableMapOf<String, Any>()
                for (i in 0 until it.columnCount) {
                    map[it.getColumnName(i)] = when (it.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_INTEGER -> it.getLong(i)
                        android.database.Cursor.FIELD_TYPE_FLOAT   -> it.getDouble(i)
                        else -> it.getString(i) ?: ""
                    }
                }
                result.add(map)
            }
        }
        return result
    }

    fun eliminarTracksSincronizados() {
        writableDatabase.delete(T_TRACKS, "sincronizado = 1", null)
    }

    fun marcarTracksSincronizados(ids: List<Long>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            ids.forEach { id ->
                val cv = ContentValues().apply { put("sincronizado", 1) }
                db.update(T_TRACKS, cv, "id = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun contarTracksPendientes(): Int {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM $T_TRACKS WHERE sincronizado = 0", null)
        cursor.use { it.moveToFirst(); return it.getInt(0) }
    }

    fun limpiarTracks() {
        writableDatabase.delete(T_TRACKS, null, null)
    }

    // ── Guardar registros de campo ────────────────────────────────────────────

    fun guardarCensoEnf(r: com.palmadata.app.censo_enfermedades.CensoEnfRegistro) {
        writableDatabase.insert(T_CENSO_ENF, null, ContentValues().apply {
            put("id", r.id); put("censo", r.censo); put("fecha", r.fecha); put("hora", r.hora); put("evaluador", r.evaluador)
            put("san_evento_enf_id", r.sanEventoEnfId); put("san_enfermedades_id", r.sanEnfermedadesId)
            put("observaciones", r.observaciones); put("linea", r.linea); put("palma", r.palma)
            put("cat_lote_id", r.catLoteId); put("cat_palma_id", r.catPalmaId); put("cat_plantacion_id", r.catPlantacionId)
            put("latitud", r.latitud); put("longitud", r.longitud); put("equipo", r.equipo); put("sincronizado", 0)
        })
    }

    fun getCensoEnfPendientes(): List<Map<String, Any>> = getPendientes(T_CENSO_ENF)
    fun eliminarCensoEnf(id: String) = writableDatabase.delete(T_CENSO_ENF, "id = ?", arrayOf(id))
    fun contarCensoEnfPendientes(): Int = contarPendientes(T_CENSO_ENF)

    fun guardarTratamiento(r: com.palmadata.app.tratamientos.TratamientoRegistro) {
        writableDatabase.insert(T_TRATAMIENTOS, null, ContentValues().apply {
            put("id", r.id); put("san_evento_trat_id", r.sanEventoTratId); put("aux_trabajador_id", r.auxTrabajadorId)
            put("fecha", r.fecha); put("hora", r.hora); put("cat_lote_id", r.catLoteId); put("cat_palma_id", r.catPalmaId)
            put("cat_plantacion_id", r.catPlantacionId); put("linea", r.linea); put("palma", r.palma)
            put("san_enfermedades_id", r.sanEnfermedadesId); put("san_evento_enf_id", r.sanEventoEnfId)
            put("observaciones", r.observaciones); put("latitud", r.latitud); put("longitud", r.longitud)
            put("cantidad", r.cantidad); put("equipo", r.equipo); put("sincronizado", 0)
        })
    }

    fun getTratamientosPendientes(): List<Map<String, Any>> = getPendientes(T_TRATAMIENTOS)
    fun eliminarTratamiento(id: String) = writableDatabase.delete(T_TRATAMIENTOS, "id = ?", arrayOf(id))
    fun contarTratamientosPendientes(): Int = contarPendientes(T_TRATAMIENTOS)

    fun guardarPolinizacion(r: com.palmadata.app.polinizacion.PolinizacionRegistro) {
        writableDatabase.insert(T_POLINIZACION, null, ContentValues().apply {
            put("id", r.id); put("fecha", r.fecha); put("hora", r.hora); put("linea", r.linea); put("palma", r.palma)
            put("cat_lote_id", r.catLoteId); put("cat_palma_id", r.catPalmaId); put("cat_plantacion_id", r.catPlantacionId)
            put("polinizador", r.polinizador); put("aplicacion1", r.aplicacion1); put("aplicacion2", r.aplicacion2)
            put("aplicacion3", r.aplicacion3); put("observaciones", r.observaciones)
            put("latitud", r.latitud); put("longitud", r.longitud); put("equipo", r.equipo); put("sincronizado", 0)
        })
    }

    fun getPolinizacionPendientes(): List<Map<String, Any>> = getPendientes(T_POLINIZACION)
    fun eliminarPolinizacion(id: String) = writableDatabase.delete(T_POLINIZACION, "id = ?", arrayOf(id))
    fun contarPolinizacionPendientes(): Int = contarPendientes(T_POLINIZACION)

    fun guardarPolen(r: com.palmadata.app.polen.PolenInicialFinalRegistro) {
        writableDatabase.insert(T_POLEN, null, ContentValues().apply {
            put("fecha", r.fecha); put("inicial", r.inicial); put("final", r.final)
            put("trabajador", r.trabajador); put("sincronizado", 0)
        })
    }

    fun getPolenPendientes(): List<Map<String, Any>> = getPendientes(T_POLEN)
    fun eliminarPolen(id: String) = writableDatabase.delete(T_POLEN, "id = ?", arrayOf(id))
    fun contarPolenPendientes(): Int = contarPendientes(T_POLEN)

    fun guardarStrategus(r: com.palmadata.app.strategus.StrategusRegistro) {
        writableDatabase.insert(T_STRATEGUS, null, ContentValues().apply {
            put("id", r.id); put("fecha", r.fecha); put("hora", r.hora)
            put("cat_lote_id", r.catLoteId); put("linea", r.linea); put("palma", r.palma)
            put("cat_palma_id", r.catPalmaId); put("galerias", r.galerias); put("censo", r.censo)
            put("evaluador", r.evaluador); put("cat_plantacion_id", r.catPlantacionId)
            put("observaciones", r.observaciones); put("latitud", r.latitud); put("longitud", r.longitud)
            put("equipo", r.equipo); put("sincronizado", 0)
        })
    }

    fun getStrateguspendientes(): List<Map<String, Any>> = getPendientes(T_STRATEGUS)
    fun eliminarStrategus(id: String) = writableDatabase.delete(T_STRATEGUS, "id = ?", arrayOf(id))
    fun contarStrateguspendientes(): Int = contarPendientes(T_STRATEGUS)

    fun guardarTrampa(r: com.palmadata.app.trampas.TrampasRegistro) {
        writableDatabase.insert(T_TRAMPAS, null, ContentValues().apply {
            put("id", r.id); put("fecha", r.fecha); put("hora", r.hora)
            put("lectura", r.lectura); put("censador", r.censador)
            put("machos", r.machos); put("hembras", r.hembras)
            put("san_trampa_id", r.sanTrampaId); put("san_tipo_trampa", r.sanTipoTrampa)
            put("cat_plantacion_id", r.catPlantacionId)
            put("atrayente", r.atrayente); put("feromona", r.feromona)
            put("observaciones", r.observaciones); put("equipo", r.equipo); put("sincronizado", 0)
        })
    }

    fun getTrampasPendientes(): List<Map<String, Any>> = getPendientes(T_TRAMPAS)
    fun eliminarTrampa(id: String) = writableDatabase.delete(T_TRAMPAS, "id = ?", arrayOf(id))
    fun contarTrampasPendientes(): Int = contarPendientes(T_TRAMPAS)

    fun guardarPlagas(r: com.palmadata.app.plagas.PlagasRegistro) {
        writableDatabase.insert(T_PLAGAS, null, ContentValues().apply {
            put("id", r.id); put("fecha", r.fecha); put("hora", r.hora)
            put("lectura", r.lectura); put("linea", r.linea); put("palma", r.palma)
            put("cat_lote_id", r.catLoteId); put("cat_palma_id", r.catPalmaId)
            put("cat_plantacion_id", r.catPlantacionId); put("evaluador", r.evaluador)
            put("insecto_id", r.insectoId); put("estado_insecto_id", r.estadoInsectoId)
            put("cantidad", r.cantidad); put("niv_foliar", r.nivFoliar)
            put("defol5", r.defol5); put("defol13", r.defol13); put("defol21", r.defol21)
            put("defol29", r.defol29); put("defol37", r.defol37)
            put("observaciones", r.observaciones)
            put("latitud", r.latitud); put("longitud", r.longitud)
            put("equipo", r.equipo); put("sincronizado", 0)
        })
    }

    fun getPlagasPendientes(): List<Map<String, Any>> = getPendientes(T_PLAGAS)
    fun eliminarPlagas(id: String) = writableDatabase.delete(T_PLAGAS, "id = ?", arrayOf(id))
    fun contarPlagasPendientes(): Int = contarPendientes(T_PLAGAS)

    fun guardarSuperCosecha(r: com.palmadata.app.supercosecha.SuperCosechaRegistro) {
        writableDatabase.insert(T_SUPER_COSECHA, null, ContentValues().apply {
            put("id_unico", r.idUnico); put("fecha", r.fecha); put("hora", r.hora)
            put("supervisor", r.supervisor); put("cortador", r.cortador); put("recolector", r.recolector)
            put("linea", r.linea); put("palma", r.palma); put("ciclo", r.ciclo)
            put("cat_lote_id", r.catLoteId); put("cat_plantacion_id", r.catPlantacionId)
            put("racimos_recogidos", r.racimosRecogidos); put("racimos_verdes", r.racimosVerdes)
            put("racimos_sobremaduros", r.racimossobremaduros); put("racimos_podridos", r.racimosPodridos)
            put("racimossinrecoger", r.racimossinrecoger); put("racimossincortar", r.racimossincortar)
            put("racimorobado", r.racimorobado); put("hojasmalacomo", r.hojasmalacomo)
            put("hojacolgando", r.hojacolgando); put("frutoplato", r.frutoplato)
            put("observaciones", r.observaciones)
            put("latitud", r.latitud); put("longitud", r.longitud)
            put("equipo", r.equipo); put("sincronizado", 0)
        })
    }

    fun getSuperCosechaPendientes(): List<Map<String, Any>> = getPendientes(T_SUPER_COSECHA)
    fun eliminarSuperCosecha(id: String) = writableDatabase.delete(T_SUPER_COSECHA, "id_unico = ?", arrayOf(id))
    fun contarSuperCosechaPendientes(): Int = contarPendientes(T_SUPER_COSECHA)

    fun guardarMaquinaria(r: com.palmadata.app.maquinaria.MaquinariaRegistro) {
        writableDatabase.insert(T_MAQUINARIA_SESION, null, ContentValues().apply {
            put("id_unico", r.idUnico); put("maquina", r.maquina); put("plantacion", r.plantacion)
            put("implemento", r.implemento); put("labor", r.labor); put("trabajador", r.trabajador)
            put("kiloinicial", r.kiloinicial); put("kilofinal", r.kilofinal)
            put("combustible", r.combustible)
            put("horometroinicial", r.horometroinicial); put("horometrofinal", r.horometrofinal)
            put("lote", r.lote); put("observaciones", r.observaciones)
            put("unidadcantidad", r.unidadcantidad); put("cantidad", r.cantidad)
            put("fechainicial", r.fechainicial); put("horainicial", r.horainicial)
            put("fechafinal", r.fechafinal); put("horafinal", r.horafinal)
            put("equipo", r.equipo); put("sincronizado", 0)
        })
    }

    fun getMaquinariaPendientes(): List<Map<String, Any>> = getPendientes(T_MAQUINARIA_SESION)
    fun eliminarMaquinaria(id: String) = writableDatabase.delete(T_MAQUINARIA_SESION, "id_unico = ?", arrayOf(id))
    fun contarMaquinariaPendientes(): Int = contarPendientes(T_MAQUINARIA_SESION)

    // ── Lecturas de maestros ──────────────────────────────────────────────────

    private fun getPendientes(tabla: String): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        val cursor = readableDatabase.query(tabla, null, "sincronizado = 0", null, null, null, "rowid ASC")
        cursor.use {
            while (it.moveToNext()) {
                val map = mutableMapOf<String, Any>()
                for (i in 0 until it.columnCount) {
                    map[it.getColumnName(i)] = when (it.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_INTEGER -> it.getLong(i)
                        android.database.Cursor.FIELD_TYPE_FLOAT   -> it.getDouble(i)
                        else -> it.getString(i) ?: ""
                    }
                }
                result.add(map)
            }
        }
        return result
    }

    private fun contarPendientes(tabla: String): Int {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM $tabla WHERE sincronizado = 0", null)
        cursor.use { it.moveToFirst(); return it.getInt(0) }
    }

    fun getPlantaciones(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_PLANTACIONES, null, null, null, null, null, "nombre")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getTrabajadores(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_TRABAJADORES, null, null, null, null, null, "nombre")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getEnfermedades(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_ENFERMEDADES, null, null, null, null, null, "nombre")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getMaquinaria(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_MAQUINARIA_MAESTRO, null, null, null, null, null, "descripcion")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getImplementos(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_IMPLEMENTOS, null, null, null, null, null, "descripcion")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getLaboresMaquinaria(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_LABORES_MAQUINARIA, null, null, null, null, null, "nombre")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getUnidadesMaquinaria(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_UNIDADES_MAQUINARIA, null, null, null, null, null, "descripcion")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getTodosLotes(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_LOTES, null, null, null, null, null, "nombre")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getTratamientosEventos(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_TRATAMIENTOS_EVT, null, null, null, null, null, "codigo")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getTrampas(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_TRAMPAS_MAESTRO, null, null, null, null, null, "codigo")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getInsectos(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_INSECTOS, null, null, null, null, null, "insecto")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getEstadosInsecto(insectoId: Int): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_ESTADOS_INSECTO, null, "insecto_id = ?", arrayOf(insectoId.toString()), null, null, "estado")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getTrabajadoresConSupervisor(): List<Triple<Int, String, Int>> {
        val result = mutableListOf<Triple<Int, String, Int>>()
        val cursor = readableDatabase.query(T_TRABAJADORES, null, null, null, null, null, "nombre")
        cursor.use { while (it.moveToNext()) result.add(Triple(it.getInt(0), it.getString(1), it.getInt(2))) }
        return result
    }

    private fun reemplazarPares(tabla: String, lista: List<Pair<Int, String>>) {
        val db = writableDatabase; db.beginTransaction()
        try { db.delete(tabla, null, null); lista.forEach { (id, nombre) -> db.insert(tabla, null, ContentValues().apply { put("id", id); put("nombre", nombre) }) }; db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    fun getSectoresPorPlantacion(plantacionId: Int): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_SECTORES, null, "plantacion_id = ?", arrayOf(plantacionId.toString()), null, null, "nombre")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getLotesPorSector(sectorId: Int): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_LOTES, null, "sector_id = ?", arrayOf(sectorId.toString()), null, null, "nombre")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getEventosPorEnfermedad(enfermedadId: Int): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_EVENTOS, null, "enfermedad_id = ?", arrayOf(enfermedadId.toString()), null, null, "codigo")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun tieneDatos(): Boolean {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM $T_TRABAJADORES", null)
        cursor.use { it.moveToFirst(); return it.getInt(0) > 0 }
    }
}