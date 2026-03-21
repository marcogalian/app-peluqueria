package com.marcog.peluqueria.citas.domain.port.out;

import com.marcog.peluqueria.citas.domain.model.Cita;

public interface NotificationPort {
    void enviarRecordatorio(Cita cita);
}
