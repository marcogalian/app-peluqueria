package com.marcog.peluqueria.citas.infrastructure.persistence;

import com.marcog.peluqueria.citas.domain.Cita;
import com.marcog.peluqueria.citas.domain.CitaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional
public class PostgresCitaRepository implements CitaRepository {

    private final JpaCitaRepository repository;
    private final CitaMapper mapper;

    @Override
    public List<Cita> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Cita> findByCriteria(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate,
            UUID peluqueroId) {
        List<CitaEntity> entities;
        if (startDate != null && endDate != null) {
            if (peluqueroId != null) {
                entities = repository.findByFechaHoraBetweenAndPeluqueroId(startDate, endDate, peluqueroId);
            } else {
                entities = repository.findByFechaHoraBetween(startDate, endDate);
            }
        } else {
            // Fallback (e.g. if no dates provided, you could return all, but we assume
            // Calendar always provides dates)
            entities = repository.findAll();
            if (peluqueroId != null) {
                entities = entities.stream()
                        .filter(e -> e.getPeluquero() != null && peluqueroId.equals(e.getPeluquero().getId()))
                        .collect(Collectors.toList());
            }
        }
        return entities.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Cita> findByClienteId(UUID clienteId) {
        return repository.findByClienteIdOrderByFechaHoraDesc(clienteId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Cita> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Cita save(Cita cita) {
        CitaEntity entity = mapper.toEntity(cita);
        CitaEntity savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
