package com.palmadata.app.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME    = "palma_data.db"
        const val DB_VERSION = 2  // incrementado por cambio en sectores

        const val T_PLANTACIONES = "plantaciones"
        const val T_TRABAJADORES = "trabajadores"
        const val T_SECTORES     = "sectores"
        const val T_LOTES        = "lotes"
        const val T_ENFERMEDADES = "enfermedades"
        const val T_EVENTOS      = "eventos"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $T_PLANTACIONES (
                id      INTEGER PRIMARY KEY,
                nombre  TEXT NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE $T_TRABAJADORES (
                id      INTEGER PRIMARY KEY,
                nombre  TEXT NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE $T_SECTORES (
                id            INTEGER PRIMARY KEY,
                nombre        TEXT NOT NULL,
                plantacion_id INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE $T_LOTES (
                id        INTEGER PRIMARY KEY,
                nombre    TEXT NOT NULL,
                sector_id INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE $T_ENFERMEDADES (
                id     INTEGER PRIMARY KEY,
                nombre TEXT NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE $T_EVENTOS (
                id            INTEGER PRIMARY KEY,
                codigo        TEXT NOT NULL,
                enfermedad_id INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        listOf(T_PLANTACIONES, T_TRABAJADORES, T_SECTORES,
            T_LOTES, T_ENFERMEDADES, T_EVENTOS).forEach {
            db.execSQL("DROP TABLE IF EXISTS $it")
        }
        onCreate(db)
    }

    // ── Insertar ──────────────────────────────────────────────────────────────

    fun reemplazarPlantaciones(lista: List<Pair<Int, String>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_PLANTACIONES, null, null)
            lista.forEach { (id, nombre) ->
                db.insert(T_PLANTACIONES, null, ContentValues().apply {
                    put("id", id); put("nombre", nombre)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun reemplazarTrabajadores(lista: List<Pair<Int, String>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_TRABAJADORES, null, null)
            lista.forEach { (id, nombre) ->
                db.insert(T_TRABAJADORES, null, ContentValues().apply {
                    put("id", id); put("nombre", nombre)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    // Triple: id, nombre, plantacion_id
    fun reemplazarSectores(lista: List<Triple<Int, String, Int>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_SECTORES, null, null)
            lista.forEach { (id, nombre, plantacionId) ->
                db.insert(T_SECTORES, null, ContentValues().apply {
                    put("id", id)
                    put("nombre", nombre)
                    put("plantacion_id", plantacionId)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun reemplazarLotes(lista: List<Triple<Int, String, Int>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_LOTES, null, null)
            lista.forEach { (id, nombre, sectorId) ->
                db.insert(T_LOTES, null, ContentValues().apply {
                    put("id", id); put("nombre", nombre); put("sector_id", sectorId)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun reemplazarEnfermedades(lista: List<Pair<Int, String>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_ENFERMEDADES, null, null)
            lista.forEach { (id, nombre) ->
                db.insert(T_ENFERMEDADES, null, ContentValues().apply {
                    put("id", id); put("nombre", nombre)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun reemplazarEventos(lista: List<Triple<Int, String, Int>>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_EVENTOS, null, null)
            lista.forEach { (id, codigo, enfId) ->
                db.insert(T_EVENTOS, null, ContentValues().apply {
                    put("id", id); put("codigo", codigo); put("enfermedad_id", enfId)
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

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

    fun getSectoresPorPlantacion(plantacionId: Int): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(
            T_SECTORES, null, "plantacion_id = ?", arrayOf(plantacionId.toString()),
            null, null, "nombre"
        )
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getLotesPorSector(sectorId: Int): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(
            T_LOTES, null, "sector_id = ?", arrayOf(sectorId.toString()),
            null, null, "nombre"
        )
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getEnfermedades(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(T_ENFERMEDADES, null, null, null, null, null, "nombre")
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun getEventosPorEnfermedad(enfermedadId: Int): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        val cursor = readableDatabase.query(
            T_EVENTOS, null, "enfermedad_id = ?", arrayOf(enfermedadId.toString()),
            null, null, "codigo"
        )
        cursor.use { while (it.moveToNext()) result.add(Pair(it.getInt(0), it.getString(1))) }
        return result
    }

    fun tieneDatos(): Boolean {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM $T_TRABAJADORES", null)
        cursor.use { it.moveToFirst(); return it.getInt(0) > 0 }
    }
}