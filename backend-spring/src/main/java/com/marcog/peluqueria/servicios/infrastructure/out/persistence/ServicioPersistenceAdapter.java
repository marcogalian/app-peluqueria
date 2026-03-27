package com.marcog.peluqueria.servicios.infrastructure.out.persistence;

import com.marcog.peluqueria.servicios.domain.model.Servicio;
import com.marcog.peluqueria.servicios.domain.port.out.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServicioPersistenceAdapter implements ServicioRepository {

    private final JpaServicioRepository repository;
    private final ServicioMapper mapper;

    @Override
    public Servicio guardar(Servicio servicio) {
        ServicioEntity entity = mapper.toEntity(servicio);
        ServicioEntity guardado = repository.save(entity);
        return mapper.toDomain(guardado);
    }

    @Override
    public Optional<Servicio> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Servicio> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
