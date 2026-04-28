package com.marcog.peluqueria.clientes.application.actualizar;

import com.marcog.peluqueria.clientes.domain.model.Cliente;
import com.marcog.peluqueria.clientes.domain.port.out.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActualizarClienteService {

    private final ClienteRepository clienteRepository;

    public Cliente actualizarCliente(UUID clienteId, Cliente clienteDetails) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (clienteDetails.getNombre() != null)
            cliente.setNombre(clienteDetails.getNombre());
        if (clienteDetails.getApellidos() != null)
            cliente.setApellidos(clienteDetails.getApellidos());
        if (clienteDetails.getTelefono() != null)
            cliente.setTelefono(clienteDetails.getTelefono());
        if (clienteDetails.getEmail() != null)
            cliente.setEmail(clienteDetails.getEmail());
        if (clienteDetails.getNotas() != null)
            cliente.setNotas(clienteDetails.getNotas());
        if (clienteDetails.getGenero() != null)
            cliente.setGenero(clienteDetails.getGenero());
        cliente.setEsVip(clienteDetails.isEsVip());

        // Actualización de historial clínico (HU11)
        if (clienteDetails.getNotasMedicas() != null)
            cliente.setNotasMedicas(clienteDetails.getNotasMedicas());
        if (clienteDetails.getFormulasTinte() != null)
            cliente.setFormulasTinte(clienteDetails.getFormulasTinte());

        return clienteRepository.save(cliente);
    }

    public Cliente actualizarConsentimiento(UUID clienteId, boolean consentimientoFotos) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        cliente.setConsentimientoFotos(consentimientoFotos);
        return clienteRepository.save(cliente);
    }

    public Cliente actualizarArchivado(UUID clienteId, boolean archivado) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        cliente.setArchivado(archivado);
        return clienteRepository.save(cliente);
    }
}
