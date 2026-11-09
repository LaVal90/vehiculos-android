package com.example.vehiculos

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var db: VehiculoDbHelper

    private lateinit var etMarca: EditText
    private lateinit var etModelo: EditText
    private lateinit var etAnio: EditText
    private lateinit var etPrecio: EditText
    private lateinit var btnGuardar: Button
    private lateinit var tvEstado: TextView
    private lateinit var tvTituloLista: TextView
    private lateinit var tvVacio: TextView
    private lateinit var llLista: LinearLayout

    // Ejecuta las operaciones de SQLite fuera del hilo de la interfaz.
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Evita que el formulario quede debajo de las barras del sistema.
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.pantalla)
        ) { view, insets ->
            val espacio = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.ime()
            )

            view.setPadding(
                espacio.left,
                espacio.top,
                espacio.right,
                espacio.bottom
            )
            insets
        }

        db = VehiculoDbHelper(applicationContext)

        etMarca = findViewById(R.id.etMarca)
        etModelo = findViewById(R.id.etModelo)
        etAnio = findViewById(R.id.etAnio)
        etPrecio = findViewById(R.id.etPrecio)
        btnGuardar = findViewById(R.id.btnGuardar)
        tvEstado = findViewById(R.id.tvEstado)
        tvTituloLista = findViewById(R.id.tvTituloLista)
        tvVacio = findViewById(R.id.tvVacio)
        llLista = findViewById(R.id.llLista)

        btnGuardar.setOnClickListener {
            guardarVehiculo()
        }

        cargarLista()
    }

    // Lee los vehículos guardados y los muestra debajo del formulario.
    private fun cargarLista() {
        executor.execute {
            try {
                val lista = db.listar()

                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
                        mostrarLista(lista)
                        mostrarEstado(null)
                    }
                }
            } catch (e: Exception) {
                Log.e("Vehiculos", "Error al consultar", e)

                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
                        mostrarEstado("No se pudo consultar la base de datos.")
                    }
                }
            }
        }
    }

    // Dibuja una tarjeta por cada vehículo guardado.
    private fun mostrarLista(lista: List<Vehiculo>) {
        llLista.removeAllViews()
        tvTituloLista.text = "Vehículos guardados (${lista.size})"
        tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE

        for (vehiculo in lista) {
            val tarjeta = layoutInflater.inflate(
                R.layout.item_vehiculo,
                llLista,
                false
            )

            tarjeta.findViewById<TextView>(R.id.tvItemTitulo).text =
                "${vehiculo.marca} ${vehiculo.modelo}"
            tarjeta.findViewById<TextView>(R.id.tvItemId).text =
                "N.º ${vehiculo.id}"
            tarjeta.findViewById<TextView>(R.id.tvItemAnio).text =
                "Año ${vehiculo.anio}"
            tarjeta.findViewById<TextView>(R.id.tvItemPrecio).text =
                formatearPrecio(vehiculo.precioCentavos)

            llLista.addView(tarjeta)
        }
    }

    // Convierte centavos a un precio legible, por ejemplo 85.000.000
    private fun formatearPrecio(centavos: Long): String {
        val pesos = BigDecimal(centavos).movePointLeft(2)
        val formato = NumberFormat.getNumberInstance(
            Locale.forLanguageTag("es-CO")
        )
        formato.minimumFractionDigits = 0
        formato.maximumFractionDigits = 2

        return "\$ " + formato.format(pesos)
    }

    // Muestra un mensaje de estado, o lo oculta si no hay nada que decir.
    private fun mostrarEstado(texto: String?) {
        tvEstado.text = texto ?: ""
        tvEstado.visibility =
            if (texto.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    private fun guardarVehiculo() {
        etMarca.error = null
        etModelo.error = null
        etAnio.error = null
        etPrecio.error = null

        val marca = etMarca.text.toString().trim()
        val modelo = etModelo.text.toString().trim()
        val anio = etAnio.text.toString().toIntOrNull()

        val precioTexto = etPrecio.text.toString()
            .trim()
            .replace(',', '.')

        val anioMaximo = Calendar.getInstance()
            .get(Calendar.YEAR) + 1

        if (marca.isBlank()) {
            etMarca.error = "Ingresa la marca"
            etMarca.requestFocus()
            return
        }

        if (modelo.isBlank()) {
            etModelo.error = "Ingresa el modelo"
            etModelo.requestFocus()
            return
        }

        if (anio == null || anio !in 1886..anioMaximo) {
            etAnio.error = "Ingresa un año entre 1886 y $anioMaximo"
            etAnio.requestFocus()
            return
        }

        if (!Regex("""\d+([.]\d{1,2})?""").matches(precioTexto)) {
            etPrecio.error = "Ingresa un precio con máximo 2 decimales"
            etPrecio.requestFocus()
            return
        }

        val precioCentavos = try {
            BigDecimal(precioTexto)
                .movePointRight(2)
                .longValueExact()
        } catch (e: ArithmeticException) {
            null
        } catch (e: NumberFormatException) {
            null
        }

        if (precioCentavos == null || precioCentavos <= 0) {
            etPrecio.error = "Ingresa un precio válido mayor que cero"
            etPrecio.requestFocus()
            return
        }

        habilitarFormulario(false)
        mostrarEstado("Guardando…")

        executor.execute {
            try {
                db.guardar(
                    marca,
                    modelo,
                    anio,
                    precioCentavos
                )

                // Vuelve a leer la lista ya con el vehículo nuevo.
                val lista = db.listar()

                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
                        etMarca.text.clear()
                        etModelo.text.clear()
                        etAnio.text.clear()
                        etPrecio.text.clear()

                        mostrarLista(lista)
                        habilitarFormulario(true)
                        mostrarEstado("Vehículo guardado correctamente.")
                        etMarca.requestFocus()
                    }
                }
            } catch (e: Exception) {
                Log.e("Vehiculos", "Error al guardar", e)

                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
                        habilitarFormulario(true)
                        mostrarEstado(
                            "No se pudo guardar. Los datos siguen en el formulario."
                        )
                    }
                }
            }
        }
    }

    private fun habilitarFormulario(habilitado: Boolean) {
        etMarca.isEnabled = habilitado
        etModelo.isEnabled = habilitado
        etAnio.isEnabled = habilitado
        etPrecio.isEnabled = habilitado
        btnGuardar.isEnabled = habilitado
    }

    override fun onDestroy() {
        // Cierra la conexión después de las operaciones pendientes.
        executor.execute {
            db.close()
        }
        executor.shutdown()
        super.onDestroy()
    }
}