package com.marcog.peluqueria.clientes.application;

import com.marcog.peluqueria.clientes.domain.Cliente;
import com.marcog.peluqueria.clientes.application.RegistrarCliente;
import com.marcog.peluqueria.clientes.domain.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Caso de uso para registrar clientes en el sistema.
 *
 * Mantiene la entrada de nuevos clientes separada del mecanismo de persistencia
 * concreto. El contrato usado por la aplicacion es {@link RegistrarCliente} y
 * el almacenamiento real se resuelve mediante {@link ClienteRepository}.
 */
@Service
@RequiredArgsConstructor
public class RegistrarClienteEnSistema implements RegistrarCliente {
    private final ClienteRepository clienteRepository;

    @Override
    public Cliente ejecutar(Cliente cliente){
        return clienteRepository.save(cliente);
    }
}
