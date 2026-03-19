package com.marcog.peluqueria.clientes.application.consultar;

import com.marcog.peluqueria.citas.application.service.CitaService;
import com.marcog.peluqueria.citas.domain.model.Cita;
import com.marcog.peluqueria.clientes.domain.model.Cliente;
import com.marcog.peluqueria.clientes.domain.model.HistorialClinicoDTO;
import com.marcog.peluqueria.clientes.domain.port.out.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultarClienteService {

    private final ClienteRepository clienteRepository;
    private final CitaService citaService;

    public List<Cliente> getAllClientes() {
        return clienteRepository.findAll();
    }

    public HistorialClinicoDTO getHistorialClinico(UUID clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        List<Cita> citasList = citaService.getCitasByCliente(clienteId);

        return HistorialClinicoDTO.builder()
                .cliente(cliente)
                .citasList(citasList)
                .notasMedicas(cliente.getNotasMedicas())
                .formulasTinte(cliente.getFormulasTinte())
                .build();
    }
}
