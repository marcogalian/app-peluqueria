package com.marcog.peluqueria.ausencias.infrastructure.persistence;

import com.marcog.peluqueria.ausencias.domain.DiaBloqueado;
import com.marcog.peluqueria.ausencias.domain.DiaBloqueadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresDiaBloqueadoRepository implements DiaBloqueadoRepository {

    private final JpaDiaBloqueadoRepository jpaRepository;

    @Override
    public List<DiaBloqueado> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public DiaBloqueado save(DiaBloqueado diaBloqueado) {
        DiaBloqueadoEntity entity = toEntity(diaBloqueado);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<DiaBloqueado> findSolapados(LocalDate inicio, LocalDate fin) {
        return jpaRepository.findSolapados(inicio, fin).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private DiaBloqueado toDomain(DiaBloqueadoEntity entity) {
        return DiaBloqueado.builder()
                .id(entity.getId())
                .fechaInicio(entity.getFechaInicio())
                .fechaFin(entity.getFechaFin())
                .motivo(entity.getMotivo())
                .build();
    }

    private DiaBloqueadoEntity toEntity(DiaBloqueado domain) {
        return DiaBloqueadoEntity.builder()
                .id(domain.getId())
                .fechaInicio(domain.getFechaInicio())
                .fechaFin(domain.getFechaFin())
                .motivo(domain.getMotivo())
                .build();
    }
}
