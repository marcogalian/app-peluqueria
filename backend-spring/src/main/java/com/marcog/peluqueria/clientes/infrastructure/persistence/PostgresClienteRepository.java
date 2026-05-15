package com.marcog.peluqueria.clientes.infrastructure.persistence;

import com.marcog.peluqueria.clientes.domain.Cliente;
import com.marcog.peluqueria.clientes.domain.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostgresClienteRepository implements ClienteRepository {

    private final JpaClienteRepository jpaRepository;
    private final ClienteMapper mapper;

    @Override
    public Cliente save(Cliente cliente) {
        ClienteEntity entity = mapper.toEntity(cliente);
        ClienteEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Cliente> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public java.util.List<Cliente> findAllByArchivado(boolean archivado) {
        return jpaRepository.findByArchivadoOrderByNombreAscApellidosAsc(archivado).stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public java.util.List<Cliente> findByFiltros(String nombre, Boolean esVip, boolean archivado) {
        return jpaRepository.findByFiltros(nombre, esVip, archivado).stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }
}
