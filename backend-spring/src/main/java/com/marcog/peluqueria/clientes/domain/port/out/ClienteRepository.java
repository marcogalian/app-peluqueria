package com.marcog.peluqueria.clientes.domain.port.out;

import com.marcog.peluqueria.clientes.domain.model.Cliente;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository {
    Cliente save(Cliente cliente);

    Optional<Cliente> findById(UUID id);

    java.util.List<Cliente> findAll();
}
