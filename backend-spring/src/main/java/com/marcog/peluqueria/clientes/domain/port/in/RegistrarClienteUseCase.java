package com.marcog.peluqueria.clientes.domain.port.in;

import com.marcog.peluqueria.clientes.domain.model.Cliente;

public interface RegistrarClienteUseCase {
    Cliente ejecutar(Cliente clieente);
}
