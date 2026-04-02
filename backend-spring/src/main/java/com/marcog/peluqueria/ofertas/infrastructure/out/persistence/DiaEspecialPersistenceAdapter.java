package com.marcog.peluqueria.ofertas.infrastructure.out.persistence;

import com.marcog.peluqueria.ofertas.domain.model.DiaEspecial;
import com.marcog.peluqueria.ofertas.domain.port.out.DiaEspecialRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List; import java.util.Optional; import java.util.UUID;
import java.util.stream.Collectors;

@Component @RequiredArgsConstructor
public class DiaEspecialPersistenceAdapter implements DiaEspecialRepositoryPort {
    private final JpaDiaEspecialRepository repository;
    private final DiaEspecialMapper mapper;

    @Override public DiaEspecial guardar(DiaEspecial d) { return mapper.toDomain(repository.save(mapper.toEntity(d))); }
    @Override public List<DiaEspecial> findAll() { return repository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList()); }
    @Override public Optional<DiaEspecial> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    @Override public void deleteById(UUID id) { repository.deleteById(id); }
}
