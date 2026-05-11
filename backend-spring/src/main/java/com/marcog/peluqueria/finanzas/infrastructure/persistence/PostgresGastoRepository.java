package com.marcog.peluqueria.finanzas.infrastructure.persistence;

import com.marcog.peluqueria.finanzas.domain.Gasto;
import com.marcog.peluqueria.finanzas.domain.GastoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresGastoRepository implements GastoRepository {

    private final JpaGastoRepository repository;
    private final GastoMapper mapper;

    @Override
    public List<Gasto> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Gasto> findByMesAndAnio(int mes, int anio) {
        return repository.findByAnioAndMes(anio, mes).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Gasto> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Gasto save(Gasto gasto) {
        GastoEntity entity = mapper.toEntity(gasto);
        GastoEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
