package com.marcog.peluqueria.fotos.infrastructure.persistence;

import com.marcog.peluqueria.fotos.domain.FotoCliente;
import com.marcog.peluqueria.fotos.domain.FotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List; import java.util.Optional; import java.util.UUID;
import java.util.stream.Collectors;

@Component @RequiredArgsConstructor
public class PostgresFotoRepository implements FotoRepository {
    private final JpaFotoRepository repository;
    private final FotoMapper mapper;

    @Override public FotoCliente guardar(FotoCliente f) { return mapper.toDomain(repository.save(mapper.toEntity(f))); }
    @Override public List<FotoCliente> findByClienteId(UUID id) { return repository.findByClienteId(id).stream().map(mapper::toDomain).collect(Collectors.toList()); }
    @Override public Optional<FotoCliente> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    @Override public void deleteById(UUID id) { repository.deleteById(id); }
}
