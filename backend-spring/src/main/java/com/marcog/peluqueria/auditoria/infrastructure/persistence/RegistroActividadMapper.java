package com.marcog.peluqueria.auditoria.infrastructure.persistence;

import com.marcog.peluqueria.auditoria.domain.RegistroActividad;

public final class RegistroActividadMapper {
    private RegistroActividadMapper() {
    }

    public static RegistroActividadEntity toEntity(RegistroActividad registro) {
        return RegistroActividadEntity.builder()
                .id(registro.getId())
                .fechaHora(registro.getFechaHora())
                .usuario(registro.getUsuario())
                .rol(registro.getRol())
                .accion(registro.getAccion())
                .modulo(registro.getModulo())
                .detalle(registro.getDetalle())
                .entidadId(registro.getEntidadId())
                .metodoHttp(registro.getMetodoHttp())
                .ruta(registro.getRuta())
                .estadoHttp(registro.getEstadoHttp())
                .build();
    }

    public static RegistroActividad toDomain(RegistroActividadEntity entity) {
        return RegistroActividad.builder()
                .id(entity.getId())
                .fechaHora(entity.getFechaHora())
                .usuario(entity.getUsuario())
                .rol(entity.getRol())
                .accion(entity.getAccion())
                .modulo(entity.getModulo())
                .detalle(entity.getDetalle())
                .entidadId(entity.getEntidadId())
                .metodoHttp(entity.getMetodoHttp())
                .ruta(entity.getRuta())
                .estadoHttp(entity.getEstadoHttp())
                .build();
    }
}
