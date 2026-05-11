package com.marcog.peluqueria.clientes.domain;

import com.marcog.peluqueria.clientes.domain.Cliente;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository {
    Cliente save(Cliente cliente);

    Optional<Cliente> findById(UUID id);

    java.util.List<Cliente> findAllByArchivado(boolean archivado);

    java.util.List<Cliente> findByFiltros(String nombre, Boolean esVip, boolean archivado);
}
