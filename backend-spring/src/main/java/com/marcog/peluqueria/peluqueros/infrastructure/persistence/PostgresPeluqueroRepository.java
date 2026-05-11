package com.marcog.peluqueria.peluqueros.infrastructure.persistence;

import com.marcog.peluqueria.peluqueros.domain.Peluquero;
import com.marcog.peluqueria.peluqueros.domain.PeluqueroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresPeluqueroRepository implements PeluqueroRepository {

    private final JpaPeluqueroRepository repository;
    private final PeluqueroMapper mapper;

    @Override
    public List<Peluquero> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Peluquero> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Peluquero> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public Peluquero save(Peluquero peluquero) {
        PeluqueroEntity entity = mapper.toEntity(peluquero);
        PeluqueroEntity savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
