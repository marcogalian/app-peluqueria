package com.marcog.peluqueria.servicios.domain.port.out;

import com.marcog.peluqueria.servicios.domain.model.Servicio;

public interface ServicioRepository {
    Servicio guardar(Servicio servicio);

    java.util.Optional<Servicio> findById(java.util.UUID id);

    java.util.List<Servicio> findAll();
    void deleteById(java.util.UUID id);
}
