package com.example.vehiculos

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Un vehículo tal como se guarda en la base de datos.
data class Vehiculo(
    val id: Long,
    val marca: String,
    val modelo: String,
    val anio: Int,
    val precioCentavos: Long
)

class VehiculoDbHelper(context: Context) :
    SQLiteOpenHelper(context, "vehiculos.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE vehiculos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                marca TEXT NOT NULL,
                modelo TEXT NOT NULL,
                anio INTEGER NOT NULL,
                precio_centavos INTEGER NOT NULL
                    CHECK (precio_centavos > 0)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Al cambiar la versión, agrega aquí una migración
        // que conserve los registros existentes.
        error("Falta implementar la migración $oldVersion → $newVersion")
    }

    fun guardar(
        marca: String,
        modelo: String,
        anio: Int,
        precioCentavos: Long
    ): Long {
        val valores = ContentValues().apply {
            put("marca", marca)
            put("modelo", modelo)
            put("anio", anio)
            put("precio_centavos", precioCentavos)
        }

        return writableDatabase.insertOrThrow(
            "vehiculos",
            null,
            valores
        )
    }

    fun contarVehiculos(): Long {
        return DatabaseUtils.queryNumEntries(
            readableDatabase,
            "vehiculos"
        )
    }

    // Devuelve todos los vehículos guardados, el más reciente primero.
    fun listar(): List<Vehiculo> {
        val lista = mutableListOf<Vehiculo>()

        readableDatabase.rawQuery(
            "SELECT id, marca, modelo, anio, precio_centavos " +
                    "FROM vehiculos ORDER BY id DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                lista.add(
                    Vehiculo(
                        id = cursor.getLong(0),
                        marca = cursor.getString(1),
                        modelo = cursor.getString(2),
                        anio = cursor.getInt(3),
                        precioCentavos = cursor.getLong(4)
                    )
                )
            }
        }

        return lista
    }
}