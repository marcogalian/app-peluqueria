package com.marcog.peluqueria.servicios.domain.port.in;

import com.marcog.peluqueria.servicios.domain.model.Servicio;

public interface CrearServicioUseCase {
    Servicio ejecutar(Servicio servicio);
}
