package com.carnecol.carnecol_auth_api.controlador;

import com.carnecol.carnecol_auth_api.modelo.Pedido;
import com.carnecol.carnecol_auth_api.servicio.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearPedido(
            @RequestBody Map<String, Object> datos) {

        String correo = String.valueOf(datos.get("correo"));

        List<Map<String, Object>> productos =
                (List<Map<String, Object>>) datos.get("productos");

        Pedido pedido = pedidoService.crearPedido(correo, productos);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Pedido registrado correctamente");
        respuesta.put("pedidoId", pedido.getId());
        respuesta.put("total", pedido.getTotal());
        respuesta.put("estado", pedido.getEstado());

        return ResponseEntity.ok(respuesta);
    }
}