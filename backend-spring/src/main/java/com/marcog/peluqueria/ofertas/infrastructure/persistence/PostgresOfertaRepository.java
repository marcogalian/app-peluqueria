package com.marcog.peluqueria.ofertas.infrastructure.persistence;

import com.marcog.peluqueria.ofertas.domain.Oferta;
import com.marcog.peluqueria.ofertas.domain.OfertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List; import java.util.Optional; import java.util.UUID;
import java.util.stream.Collectors;

@Component @RequiredArgsConstructor
public class PostgresOfertaRepository implements OfertaRepository {
    private final JpaOfertaRepository repository;
    private final OfertaMapper mapper;

    @Override public Oferta guardar(Oferta o) { return mapper.toDomain(repository.save(mapper.toEntity(o))); }
    @Override public Optional<Oferta> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    @Override public List<Oferta> findAll() { return repository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList()); }
    @Override public List<Oferta> findActivas() { return repository.findActivas(LocalDate.now()).stream().map(mapper::toDomain).collect(Collectors.toList()); }
    @Override public void deleteById(UUID id) { repository.deleteById(id); }
}
