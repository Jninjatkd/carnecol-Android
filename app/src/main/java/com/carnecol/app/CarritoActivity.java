package com.carnecol.app;

import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CarritoActivity extends AppCompatActivity {

    private LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarCarrito();
    }

    private void mostrarCarrito() {

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 40, 30, 30);
        layout.setBackgroundColor(0xFFF5F5F5);

        TextView titulo = new TextView(this);
        titulo.setText("MI CARRITO");
        titulo.setTextSize(26);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setTextColor(0xFF971B25);
        titulo.setGravity(Gravity.CENTER);
        layout.addView(titulo);

        Map<String, Integer> productos = Carrito.obtenerProductos();

        if (productos.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText("Tu carrito está vacío");
            vacio.setTextSize(18);
            vacio.setGravity(Gravity.CENTER);
            vacio.setPadding(0, 40, 0, 40);
            layout.addView(vacio);
        } else {
            for (Map.Entry<String, Integer> producto : productos.entrySet()) {
                TextView item = new TextView(this);
                item.setText(producto.getKey() + "  x" + producto.getValue());
                item.setTextSize(18);
                item.setPadding(10, 15, 10, 15);
                layout.addView(item);
            }
        }

        TextView total = new TextView(this);
        total.setText("Total de unidades: " + Carrito.obtenerCantidadTotal());
        total.setTextSize(18);
        total.setPadding(0, 20, 0, 20);
        layout.addView(total);

        Button confirmar = new Button(this);
        confirmar.setText("CONFIRMAR PEDIDO");
        layout.addView(confirmar);

        confirmar.setEnabled(!productos.isEmpty());

        confirmar.setOnClickListener(v -> confirmarPedido(confirmar));

        Button vaciar = new Button(this);
        vaciar.setText("VACIAR CARRITO");
        layout.addView(vaciar);

        vaciar.setOnClickListener(v -> {
            Carrito.vaciarCarrito();
            mostrarCarrito();
        });

        Button regresar = new Button(this);
        regresar.setText("VOLVER AL INICIO");
        layout.addView(regresar);

        regresar.setOnClickListener(v -> finish());

        setContentView(layout);
    }

    private void confirmarPedido(Button boton) {

        SharedPreferences preferencias =
                getSharedPreferences("CarnecolUsuarios", MODE_PRIVATE);

        String correo = preferencias.getString("sesion_email", null);

        if (correo == null || correo.isEmpty()) {
            Toast.makeText(this,
                    "Inicie sesión para confirmar el pedido",
                    Toast.LENGTH_LONG).show();

            startActivity(new Intent(this, AccesoActivity.class));
            return;
        }

        Map<String, Integer> productos = Carrito.obtenerProductos();

        if (productos.isEmpty()) {
            Toast.makeText(this, "El carrito está vacío",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boton.setEnabled(false);
        boton.setText("PROCESANDO...");

        new Thread(() -> {

            HttpURLConnection conexion = null;

            try {
                URL url = new URL("http://10.0.2.2:8080/api/pedidos");
                conexion = (HttpURLConnection) url.openConnection();

                conexion.setRequestMethod("POST");
                conexion.setRequestProperty("Content-Type", "application/json");
                conexion.setConnectTimeout(10000);
                conexion.setReadTimeout(10000);
                conexion.setDoOutput(true);

                JSONObject datos = new JSONObject();
                datos.put("correo", correo);

                JSONArray listaProductos = new JSONArray();

                for (Map.Entry<String, Integer> producto : productos.entrySet()) {
                    JSONObject item = new JSONObject();
                    item.put("nombre", producto.getKey());
                    item.put("cantidad", producto.getValue());
                    listaProductos.put(item);
                }

                datos.put("productos", listaProductos);

                try (OutputStream salida = conexion.getOutputStream()) {
                    salida.write(datos.toString().getBytes(StandardCharsets.UTF_8));
                }

                int codigo = conexion.getResponseCode();

                InputStream flujo = codigo >= 200 && codigo < 300
                        ? conexion.getInputStream()
                        : conexion.getErrorStream();

                StringBuilder respuesta = new StringBuilder();

                if (flujo != null) {
                    try (BufferedReader lector = new BufferedReader(
                            new InputStreamReader(flujo, StandardCharsets.UTF_8))) {

                        String linea;
                        while ((linea = lector.readLine()) != null) {
                            respuesta.append(linea);
                        }
                    }
                }

                if (codigo >= 200 && codigo < 300) {

                    JSONObject resultado = new JSONObject(respuesta.toString());

                    String mensaje = resultado.optString(
                            "mensaje", "Pedido registrado correctamente");

                    String numeroPedido = resultado.optString("pedidoId", "");

                    Carrito.vaciarCarrito();

                    runOnUiThread(() -> {
                        Toast.makeText(this,
                                mensaje + " - Pedido #" + numeroPedido,
                                Toast.LENGTH_LONG).show();
                        mostrarCarrito();
                    });

                } else {

                    String mensaje = "No se pudo registrar el pedido";

                    try {
                        JSONObject error = new JSONObject(respuesta.toString());
                        mensaje = error.optString("detail", mensaje);
                    } catch (Exception ignored) {}

                    String mensajeFinal = mensaje;

                    runOnUiThread(() -> {
                        boton.setEnabled(true);
                        boton.setText("CONFIRMAR PEDIDO");

                        Toast.makeText(this, mensajeFinal,
                                Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {

                runOnUiThread(() -> {
                    boton.setEnabled(true);
                    boton.setText("CONFIRMAR PEDIDO");

                    Toast.makeText(this,
                            "Error de conexión con el servidor",
                            Toast.LENGTH_LONG).show();
                });

            } finally {
                if (conexion != null) {
                    conexion.disconnect();
                }
            }

        }).start();
    }
}
