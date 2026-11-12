package com.carnecol.app;

import java.util.LinkedHashMap;
import java.util.Map;

public class Carrito {

    private static final Map<String, Integer> productos = new LinkedHashMap<>();

    public static void agregarProducto(String nombre) {
        int cantidad = productos.containsKey(nombre)
                ? productos.get(nombre) + 1 : 1;

        productos.put(nombre, cantidad);
    }

    public static int obtenerCantidadTotal() {
        int total = 0;

        for (int cantidad : productos.values()) {
            total += cantidad;
        }

        return total;
    }

    public static Map<String, Integer> obtenerProductos() {
        return new LinkedHashMap<>(productos);
    }

    public static void vaciarCarrito() {
        productos.clear();
    }
}
