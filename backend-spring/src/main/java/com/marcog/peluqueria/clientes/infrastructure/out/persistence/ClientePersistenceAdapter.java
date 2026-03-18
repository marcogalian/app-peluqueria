package com.marcog.peluqueria.clientes.infrastructure.out.persistence;

import com.marcog.peluqueria.clientes.domain.model.Cliente;
import com.marcog.peluqueria.clientes.domain.port.out.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientePersistenceAdapter implements ClienteRepository {

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
    public java.util.List<Cliente> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }
}
