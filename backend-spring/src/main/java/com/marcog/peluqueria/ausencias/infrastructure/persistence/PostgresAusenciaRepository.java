package com.marcog.peluqueria.ausencias.infrastructure.persistence;

import com.marcog.peluqueria.ausencias.domain.SolicitudAusencia;
import com.marcog.peluqueria.ausencias.domain.AusenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List; import java.util.Optional; import java.util.UUID;
import java.util.stream.Collectors;

@Component @RequiredArgsConstructor
public class PostgresAusenciaRepository implements AusenciaRepository {
    private final JpaAusenciaRepository repository;
    private final AusenciaMapper mapper;

    @Override public SolicitudAusencia guardar(SolicitudAusencia s) { return mapper.toDomain(repository.save(mapper.toEntity(s))); }
    @Override public Optional<SolicitudAusencia> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    @Override public List<SolicitudAusencia> findAll() { return repository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList()); }
    @Override public List<SolicitudAusencia> findByPeluqueroId(UUID id) { return repository.findByPeluqueroId(id).stream().map(mapper::toDomain).collect(Collectors.toList()); }
}
