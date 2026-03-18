package com.marcog.peluqueria.clientes.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JpaClienteRepository extends JpaRepository<ClienteEntity, UUID> {

}
