package com.marcog.peluqueria.clientes.application;

import com.marcog.peluqueria.clientes.domain.Cliente;
import com.marcog.peluqueria.clientes.application.RegistrarCliente;
import com.marcog.peluqueria.clientes.domain.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrarClienteEnSistema implements RegistrarCliente {
    private final ClienteRepository clienteRepository;

    @Override
    public Cliente ejecutar(Cliente cliente){
        return clienteRepository.save(cliente);
    }
}
