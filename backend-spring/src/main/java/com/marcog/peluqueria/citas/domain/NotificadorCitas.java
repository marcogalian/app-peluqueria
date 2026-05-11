package com.marcog.peluqueria.citas.domain;

import com.marcog.peluqueria.citas.domain.Cita;

public interface NotificadorCitas {
    void enviarRecordatorio(Cita cita);
}
