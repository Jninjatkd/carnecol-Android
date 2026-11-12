package com.carnecol.carnecol_auth_api.servicio;

import com.carnecol.carnecol_auth_api.modelo.*;
import com.carnecol.carnecol_auth_api.repositorio.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            UsuarioRepository usuarioRepository,
            ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public Pedido crearPedido(String correo, List<Map<String, Object>> productos) {

        Usuario usuario = usuarioRepository.findByUsuario(correo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (productos == null || productos.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El carrito está vacío");
        }

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado("Pendiente");

        BigDecimal total = BigDecimal.ZERO;

        for (Map<String, Object> item : productos) {

            String nombre = String.valueOf(item.get("nombre"));

            int cantidad;
            try {
                cantidad = Integer.parseInt(String.valueOf(item.get("cantidad")));
            } catch (Exception e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Cantidad inválida");
            }

            if (cantidad <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor que cero");
            }

            Producto producto = productoRepository.findByNombreIgnoreCase(nombre)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Producto no encontrado: " + nombre));

            if (producto.getStock() < cantidad) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para: " + producto.getNombre());
            }

            BigDecimal precio = producto.getPrecio();
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad));

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precio);
            detalle.setSubtotal(subtotal);

            pedido.getDetalles().add(detalle);

            producto.setStock(producto.getStock() - cantidad);
            productoRepository.save(producto);

            total = total.add(subtotal);
        }

        pedido.setTotal(total);

        return pedidoRepository.save(pedido);
    }
}