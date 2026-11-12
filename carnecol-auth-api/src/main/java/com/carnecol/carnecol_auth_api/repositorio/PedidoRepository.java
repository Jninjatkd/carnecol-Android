package com.carnecol.carnecol_auth_api.repositorio;

import com.carnecol.carnecol_auth_api.modelo.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}