package com.carnecol.app;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AccesoActivity extends AppCompatActivity {

    private EditText nombre, correo, clave;
    private SharedPreferences preferencias;
    private boolean registrando = false;

    private static final String URL_API =
            "http://10.0.2.2:8080/api/auth/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferencias = getSharedPreferences(
                "CarnecolUsuarios", MODE_PRIVATE);

        if (preferencias.contains("sesion_email")) {
            mostrarPerfil();
        } else {
            mostrarPantalla();
        }
    }

    private void mostrarPantalla() {

        LinearLayout layout = crearLayout();

        layout.addView(crearTexto("CARNECOL", 30, true));

        layout.addView(crearTexto(
                registrando ? "Crea tu cuenta" : "Accede a tu cuenta",
                18, false));

        nombre = new EditText(this);
        nombre.setHint("Nombre completo");
        nombre.setVisibility(registrando ? View.VISIBLE : View.GONE);
        layout.addView(nombre);

        correo = new EditText(this);
        correo.setHint("Correo electrónico");
        correo.setInputType(33);
        layout.addView(correo);

        clave = new EditText(this);
        clave.setHint("Contraseña");
        clave.setInputType(129);
        layout.addView(clave);

        Button accion = crearBoton(
                registrando ? "REGISTRARME" : "INICIAR SESIÓN");
        layout.addView(accion);

        Button cambiar = crearBoton(
                registrando
                        ? "¿Ya tienes cuenta? Inicia sesión"
                        : "¿No tienes cuenta? Regístrate");
        layout.addView(cambiar);

        Button volver = crearBoton("VOLVER AL INICIO");
        layout.addView(volver);

        cambiar.setOnClickListener(v -> {
            registrando = !registrando;
            mostrarPantalla();
        });

        volver.setOnClickListener(v -> finish());

        accion.setOnClickListener(v -> procesarAccion(accion));

        setContentView(layout);
    }

    private void procesarAccion(Button boton) {

        String c = correo.getText().toString()
                .trim().toLowerCase();

        String p = clave.getText().toString();

        String n = registrando
                ? nombre.getText().toString().trim()
                : "";

        if (c.isEmpty() || p.trim().isEmpty()
                || (registrando && n.isEmpty())) {

            Toast.makeText(this,
                    "Complete todos los campos",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boton.setEnabled(false);

        new Thread(() -> {

            try {

                String endpoint = registrando
                        ? "registro" : "login";

                JSONObject datos = new JSONObject();
                datos.put("usuario", c);
                datos.put("contrasena", p);

                URL url = new URL(URL_API + endpoint);

                HttpURLConnection conexion =
                        (HttpURLConnection) url.openConnection();

                conexion.setRequestMethod("POST");
                conexion.setConnectTimeout(10000);
                conexion.setReadTimeout(10000);
                conexion.setDoOutput(true);
                conexion.setRequestProperty(
                        "Content-Type", "application/json");
                conexion.setRequestProperty(
                        "Accept", "application/json");

                try (OutputStream salida =
                             conexion.getOutputStream()) {

                    salida.write(datos.toString().getBytes(
                            StandardCharsets.UTF_8));
                }

                int codigo = conexion.getResponseCode();

                InputStream flujo = codigo >= 200 && codigo < 300
                        ? conexion.getInputStream()
                        : conexion.getErrorStream();

                StringBuilder respuesta = new StringBuilder();

                if (flujo != null) {
                    try (BufferedReader lector =
                                 new BufferedReader(
                                         new InputStreamReader(
                                                 flujo,
                                                 StandardCharsets.UTF_8))) {

                        String linea;

                        while ((linea = lector.readLine()) != null) {
                            respuesta.append(linea);
                        }
                    }
                }

                conexion.disconnect();

                JSONObject resultado =
                        new JSONObject(respuesta.toString());

                String mensaje =
                        resultado.optString("mensaje", "");

                runOnUiThread(() -> {

                    boton.setEnabled(true);

                    if (codigo >= 200 && codigo < 300) {

                        if (registrando) {

                            preferencias.edit()
                                    .putString("nombre_" + c, n)
                                    .apply();

                            Toast.makeText(this,
                                    mensaje,
                                    Toast.LENGTH_LONG).show();

                            registrando = false;
                            mostrarPantalla();

                        } else if (mensaje.toLowerCase()
                                .contains("satisfactoria")) {

                            preferencias.edit()
                                    .putString("sesion_email", c)
                                    .apply();

                            mostrarPerfil();

                        } else {

                            Toast.makeText(this,
                                    mensaje,
                                    Toast.LENGTH_LONG).show();
                        }

                    } else {

                        Toast.makeText(this,
                                mensaje.isEmpty()
                                        ? "No se pudo completar la solicitud"
                                        : mensaje,
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    boton.setEnabled(true);

                    Toast.makeText(this,
                            "Error de conexión. Verifique que el servidor esté funcionando.",
                            Toast.LENGTH_LONG).show();
                });
            }

        }).start();
    }

    private void mostrarPerfil() {

        String email = preferencias.getString(
                "sesion_email", "");

        String n = preferencias.getString(
                "nombre_" + email, "Cliente");

        LinearLayout layout = crearLayout();

        layout.addView(crearTexto("MI PERFIL", 28, true));

        layout.addView(crearTexto(
                "Bienvenido, " + n, 21, true));

        layout.addView(crearTexto(
                "Correo: " + email, 16, false));

        Button cerrarSesion = crearBoton("CERRAR SESIÓN");
        layout.addView(cerrarSesion);

        Button volver = crearBoton("VOLVER AL INICIO");
        layout.addView(volver);

        cerrarSesion.setOnClickListener(v -> {
            preferencias.edit()
                    .remove("sesion_email")
                    .apply();

            registrando = false;
            mostrarPantalla();
        });

        volver.setOnClickListener(v -> finish());

        setContentView(layout);
    }

    private LinearLayout crearLayout() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(35, 45, 35, 35);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setBackgroundColor(0xFFF5F5F5);

        return layout;
    }

    private TextView crearTexto(
            String texto, int tamaño, boolean negrita) {

        TextView vista = new TextView(this);
        vista.setText(texto);
        vista.setTextSize(tamaño);
        vista.setTextColor(0xFF971B25);
        vista.setGravity(Gravity.CENTER);
        vista.setPadding(0, 12, 0, 20);

        if (negrita) {
            vista.setTypeface(null, Typeface.BOLD);
        }

        return vista;
    }

    private Button crearBoton(String texto) {

        Button boton = new Button(this);
        boton.setText(texto);
        boton.setTextColor(0xFFFFFFFF);
        boton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        0xFF971B25));

        return boton;
    }
}
