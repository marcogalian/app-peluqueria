package com.marcog.peluqueria.auditoria.infrastructure.persistence;

import com.marcog.peluqueria.auditoria.domain.RegistroActividad;
import com.marcog.peluqueria.auditoria.domain.RegistroActividadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostgresRegistroActividadRepository implements RegistroActividadRepository {
    private final JpaRegistroActividadRepository jpaRepository;

    @Override
    public RegistroActividad guardar(RegistroActividad registro) {
        return RegistroActividadMapper.toDomain(
                jpaRepository.save(RegistroActividadMapper.toEntity(registro))
        );
    }

    @Override
    public List<RegistroActividad> listarRecientes(int limite) {
        return jpaRepository.findTop200ByOrderByFechaHoraDesc().stream()
                .limit(limite)
                .map(RegistroActividadMapper::toDomain)
                .toList();
    }
}
