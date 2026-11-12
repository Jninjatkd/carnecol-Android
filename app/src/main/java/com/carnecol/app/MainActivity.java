package com.carnecol.app;

import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private LinearLayout listaProductos;
    private EditText buscador;
    private final ArrayList<JSONObject> productos = new ArrayList<>();

    private final String DIRECCION_API =
            "http://10.0.2.2:8080/api/productos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prepararCatalogo();

        buscador = findViewById(R.id.buscador);

        TextView btnAbrirCarrito =
                findViewById(R.id.btnAbrirCarrito);

        TextView btnPerfil =
                findViewById(R.id.btnPerfil);

        btnAbrirCarrito.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this, CarritoActivity.class);
            startActivity(intent);
        });

        btnPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this, AccesoActivity.class);
            startActivity(intent);
        });

        buscador.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                filtrarProductos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        cargarProductos();
    }

    private void prepararCatalogo() {

        LinearLayout contenido =
                findViewById(R.id.contenedorContenido);

        int indiceTitulo = -1;

        for (int i = 0; i < contenido.getChildCount(); i++) {
            View vista = contenido.getChildAt(i);

            if (vista instanceof TextView) {
                String texto = ((TextView) vista)
                        .getText().toString().trim();

                if (texto.equals("Nuestros productos")) {
                    indiceTitulo = i;
                    break;
                }
            }
        }

        if (indiceTitulo == -1) {
            return;
        }

        for (int i = contenido.getChildCount() - 1;
             i > indiceTitulo; i--) {
            contenido.removeViewAt(i);
        }

        listaProductos = new LinearLayout(this);
        listaProductos.setOrientation(LinearLayout.VERTICAL);

        contenido.addView(listaProductos);
    }

    private void cargarProductos() {

        new Thread(() -> {

            HttpURLConnection conexion = null;

            try {
                URL url = new URL(DIRECCION_API);

                conexion = (HttpURLConnection) url.openConnection();
                conexion.setRequestMethod("GET");
                conexion.setConnectTimeout(8000);
                conexion.setReadTimeout(8000);

                int codigo = conexion.getResponseCode();

                if (codigo != 200) {
                    throw new Exception("Error del servidor: " + codigo);
                }

                BufferedReader lector = new BufferedReader(
                        new InputStreamReader(
                                conexion.getInputStream(), "UTF-8"));

                StringBuilder respuesta = new StringBuilder();
                String linea;

                while ((linea = lector.readLine()) != null) {
                    respuesta.append(linea);
                }

                lector.close();

                JSONArray arreglo =
                        new JSONArray(respuesta.toString());

                runOnUiThread(() -> mostrarProductos(arreglo));

            } catch (Exception e) {

                runOnUiThread(() ->
                        mostrarMensaje("No fue posible cargar los productos. "
                                + "Verifique que el servidor esté activo."));
            } finally {
                if (conexion != null) {
                    conexion.disconnect();
                }
            }

        }).start();
    }

    private void mostrarProductos(JSONArray arreglo) {

        productos.clear();
        listaProductos.removeAllViews();

        if (arreglo.length() == 0) {
            mostrarMensaje("Todavía no hay productos registrados.");
            return;
        }

        for (int i = 0; i < arreglo.length(); i++) {
            JSONObject producto = arreglo.optJSONObject(i);

            if (producto != null) {
                productos.add(producto);
                crearTarjeta(producto);
            }
        }
    }

    private void crearTarjeta(JSONObject producto) {

        String nombre = producto.optString("nombre", "Producto");
        String descripcion =
                producto.optString("descripcion", "Producto Carnecol");
        String categoria = producto.optString("categoria", "");
        String precio = producto.optString("precio", "0");
        int stock = producto.optInt("stock", 0);

        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setBackgroundColor(Color.WHITE);
        tarjeta.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout.LayoutParams parametros =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        parametros.setMargins(0, dp(15), 0, 0);
        tarjeta.setLayoutParams(parametros);

        int imagen = obtenerImagen(nombre, categoria);

        if (imagen != 0) {
            ImageView foto = new ImageView(this);
            foto.setImageResource(imagen);
            foto.setScaleType(ImageView.ScaleType.CENTER_CROP);

            tarjeta.addView(foto, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(175)));
        }

        TextView titulo = crearTexto(nombre, 19, true);
        titulo.setPadding(0, dp(12), 0, 0);
        tarjeta.addView(titulo);

        TextView detalle = crearTexto(descripcion, 14, false);
        detalle.setTextColor(Color.GRAY);
        detalle.setPadding(0, dp(5), 0, 0);
        tarjeta.addView(detalle);

        TextView valor = crearTexto(
                "$" + formatoPrecio(precio), 21, true);
        valor.setTextColor(Color.rgb(151, 27, 37));
        valor.setPadding(0, dp(10), 0, 0);
        tarjeta.addView(valor);

        TextView existencia = crearTexto(
                "Disponibles: " + stock, 14, false);
        existencia.setTextColor(Color.DKGRAY);
        tarjeta.addView(existencia);

        Button agregar = new Button(this);
        agregar.setText("Agregar al carrito");
        agregar.setTextColor(Color.WHITE);
        agregar.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.rgb(151, 27, 37)));

        agregar.setOnClickListener(v -> {
            Carrito.agregarProducto(nombre);
        });

        tarjeta.addView(agregar);

        tarjeta.setTag((nombre + " " + categoria + " "
                + descripcion).toLowerCase(Locale.ROOT));

        listaProductos.addView(tarjeta);
    }

    private TextView crearTexto(
            String contenido, int tamano, boolean negrita) {

        TextView texto = new TextView(this);
        texto.setText(contenido);
        texto.setTextColor(Color.rgb(34, 34, 34));
        texto.setTextSize(tamano);

        if (negrita) {
            texto.setTypeface(null,
                    android.graphics.Typeface.BOLD);
        }

        return texto;
    }

    private int obtenerImagen(String nombre, String categoria) {

        String referencia =
                (nombre + " " + categoria).toLowerCase(Locale.ROOT);

        if (referencia.contains("pollo")) {
            return R.drawable.pollo;
        }

        if (referencia.contains("cerdo")) {
            return R.drawable.cerdo;
        }

        if (referencia.contains("pescado")) {
            return R.drawable.pescado;
        }

        return 0;
    }

    private String formatoPrecio(String precio) {

        try {
            BigDecimal valor = new BigDecimal(precio);

            NumberFormat formato =
                    NumberFormat.getNumberInstance(
                            new Locale("es", "CO"));

            formato.setMaximumFractionDigits(0);

            return formato.format(valor);

        } catch (Exception e) {
            return precio;
        }
    }

    private void filtrarProductos(String busqueda) {

        if (listaProductos == null) {
            return;
        }

        String texto = busqueda.toLowerCase(Locale.ROOT).trim();

        for (int i = 0; i < listaProductos.getChildCount(); i++) {
            View tarjeta = listaProductos.getChildAt(i);

            String contenido = tarjeta.getTag() == null
                    ? ""
                    : tarjeta.getTag().toString();

            tarjeta.setVisibility(
                    contenido.contains(texto)
                            ? View.VISIBLE : View.GONE);
        }
    }

    private void mostrarMensaje(String mensaje) {

        listaProductos.removeAllViews();

        TextView aviso = crearTexto(mensaje, 16, false);
        aviso.setPadding(0, dp(20), 0, dp(20));

        listaProductos.addView(aviso);
    }

    private int dp(int valor) {
        return (int) (valor * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}
