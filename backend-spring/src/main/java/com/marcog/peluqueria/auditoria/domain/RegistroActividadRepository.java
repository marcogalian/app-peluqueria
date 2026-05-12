package com.marcog.peluqueria.auditoria.domain;

import java.util.List;

public interface RegistroActividadRepository {
    RegistroActividad guardar(RegistroActividad registro);

    List<RegistroActividad> listarRecientes(int limite);
}
