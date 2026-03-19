package com.marcog.peluqueria.clientes.application.registrar;

import com.marcog.peluqueria.clientes.domain.model.Cliente;
import com.marcog.peluqueria.clientes.domain.port.in.RegistrarClienteUseCase;
import com.marcog.peluqueria.clientes.domain.port.out.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrarClienteService implements RegistrarClienteUseCase {
    private final ClienteRepository clienteRepository;

    @Override
    public Cliente ejecutar(Cliente cliente){
        // TODO logica de negocio
        // teléfono existe?, normalizar el nombre, etc.
        System.out.println("Registrando cliente: " + cliente.getNombre());

        return clienteRepository.save(cliente);
    }
}
